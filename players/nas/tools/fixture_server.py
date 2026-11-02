#!/usr/bin/env python3
"""Protocol fixture, not a Jellyfin/Emby server. Binds loopback for adb reverse only.
Generate a synthetic MP4 with make_fixture.sh first. Credentials: fixture / fixture.
Requests are logged without tokens, passwords, addresses or video URLs.
"""
import argparse, json, re, threading, time
from http.server import ThreadingHTTPServer, BaseHTTPRequestHandler
from pathlib import Path
from urllib.parse import urlparse, parse_qs

parser=argparse.ArgumentParser()
parser.add_argument('--port',type=int,default=18967)
parser.add_argument('--media-dir',type=Path,default=Path(__file__).resolve().parent.parent/'local/fixture')
args=parser.parse_args()
positions={}
lock=threading.Lock()

def media(i):
    series=i=='series-1'
    return {'Id':i,'Name':{'film-1':'Aether · Signal','film-2':'Aether · Nightfall','series-1':'Aether · Journeys','episode-1':'Episode 1 · Arrival'}.get(i,'Aether · Signal'),'Type':'Series' if series else ('Episode' if i.startswith('episode') else 'Movie'),'Overview':'Synthetic protocol fixture for AetherCinema. This is generated test media, not a connected household library.','ProductionYear':2026,'RunTimeTicks':900000000,'ImageTags':{'Primary':i},'UserData':{'PlaybackPositionTicks':positions.get(i,0)}}

class Handler(BaseHTTPRequestHandler):
    protocol_version='HTTP/1.1'
    def log_message(self,*a): pass
    def send(self,value,status=200,kind='application/json'):
        data=json.dumps(value).encode() if kind=='application/json' else value
        self.send_response(status);self.send_header('Content-Type',kind);self.send_header('Content-Length',str(len(data)));self.end_headers();self.wfile.write(data)
    def route(self):
        parsed=urlparse(self.path); path=parsed.path
        emby=path.startswith('/emby/')
        path=re.sub('^/emby','',path)
        q=parse_qs(parsed.query)
        data={}
        if self.command=='POST':
            data=json.loads(self.rfile.read(int(self.headers.get('Content-Length','0'))) or '{}')
        if path=='/System/Info/Public': return self.send({'ProductName':'Emby Server' if emby else 'Jellyfin Server','Version':'4.8.11.0' if emby else '10.10.7'})
        if path=='/Users/AuthenticateByName':
            if data.get('Username')!='fixture' or data.get('Pw')!='fixture': return self.send({},401)
            print(json.dumps({'event':'login','provider':'emby' if emby else 'jellyfin'}),flush=True)
            return self.send({'AccessToken':'fixture-token','User':{'Id':'fixture-user','Name':'Fixture'}})
        header=self.headers.get('X-Emby-Token','') if emby else self.headers.get('Authorization','')
        if 'fixture-token' not in header and q.get('api_key')!=['fixture-token']: return self.send({},401)
        if '/Images/Primary' in path:
            i=path.split('/')[2]; f=args.media_dir/(i+'.jpg')
            return self.send(f.read_bytes(),kind='image/jpeg') if f.exists() else self.send({},404)
        if path=='/Users/fixture-user/Views': return self.send({'Items':[{'Id':'library-1','Name':'Fixture library','Type':'CollectionFolder'}]})
        if path=='/Users/fixture-user/Items/Resume': return self.send({'Items':[media(i) for i in positions if positions[i]>0]})
        if path=='/Users/fixture-user/Items':
            items=[media('episode-1')] if q.get('ParentId')==['series-1'] else [media(i) for i in ['film-1','film-2','series-1']]
            if 'SearchTerm' in q: items=[x for x in items if q['SearchTerm'][0].lower() in x['Name'].lower()]
            return self.send({'Items':items,'TotalRecordCount':len(items)})
        if path.startswith('/Users/fixture-user/Items/'): return self.send(media(path.split('/')[-1]))
        if path.endswith('/PlaybackInfo'):
            i=path.split('/')[2]
            direct=data.get('EnableDirectPlay',True)
            print(json.dumps({'event':'negotiate','provider':'emby' if emby else 'jellyfin','method':'direct' if direct else 'transcode','start':data.get('StartTimeTicks')}),flush=True)
            return self.send({'PlaySessionId':'fixture-session-'+i,'MediaSources':[{'Id':'source-'+i,'Protocol':'File','Container':'mp4','SupportsDirectPlay':direct,'SupportsDirectStream':False,'SupportsTranscoding':True,'TranscodingUrl':('/emby' if emby else '')+'/hls/master.m3u8?api_key=fixture-token','MediaStreams':[{'Type':'Video','Index':0,'Codec':'h264'},{'Type':'Audio','Index':1,'Codec':'aac','Language':'eng'},{'Type':'Subtitle','Index':2,'IsExternal':True,'Codec':'srt','Language':'eng','DisplayTitle':'Fixture English'}]}]})
        if path.startswith('/Sessions/'):
            with lock: positions[data.get('ItemId','film-1')]=data.get('PositionTicks',0)
            print(json.dumps({'event':path,'position':data.get('PositionTicks'),'paused':data.get('IsPaused')}),flush=True)
            return self.send({})
        if '/Subtitles/' in path: return self.send(b'WEBVTT\n\n00:00.000 --> 01:30.000\nAetherCinema protocol fixture\n',kind='text/vtt')
        if path=='/Videos/ActiveEncodings': return self.send({})
        if path.startswith('/hls/'):
            f=args.media_dir/'hls'/Path(path).name
            if not f.is_file(): return self.send({},404)
            content=f.read_bytes()
            if f.suffix=='.m3u8': content=re.sub(rb'(segment\d+\.ts)',rb'\1?api_key=fixture-token',content)
            return self.send(content,kind='application/vnd.apple.mpegurl' if f.suffix=='.m3u8' else 'video/mp2t')
        if path.startswith('/Videos/') and path.endswith('/stream'):
            f=args.media_dir/'fixture.mp4'; size=f.stat().st_size
            start,end=0,size-1
            match=re.match(r'bytes=(\d+)-(\d*)',self.headers.get('Range',''))
            if match: start=int(match[1]);end=min(int(match[2]) if match[2] else end,end)
            self.send_response(206 if match else 200);self.send_header('Content-Type','video/mp4');self.send_header('Accept-Ranges','bytes');self.send_header('Content-Length',str(end-start+1))
            if match: self.send_header('Content-Range',f'bytes {start}-{end}/{size}')
            self.end_headers()
            with f.open('rb') as fp:
                fp.seek(start); remaining=end-start+1
                while remaining>0:
                    b=fp.read(min(65536,remaining));self.wfile.write(b);remaining-=len(b)
            return
        return self.send({},404)
    def do_GET(self):
        try:self.route()
        except (BrokenPipeError,ConnectionResetError):pass
    do_POST=do_GET
    do_DELETE=do_GET

print('AetherCinema protocol fixture ready on loopback',flush=True)
ThreadingHTTPServer(('127.0.0.1',args.port),Handler).serve_forever()

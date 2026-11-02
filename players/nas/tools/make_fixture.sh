#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
mkdir -p local/fixture/hls
ffmpeg -hide_banner -loglevel error -y -f lavfi -i 'testsrc2=size=1280x720:rate=24:duration=90' -f lavfi -i 'sine=frequency=440:sample_rate=48000:duration=90' -c:v libx264 -preset veryfast -pix_fmt yuv420p -profile:v main -level 3.1 -b:v 1400k -c:a aac -b:a 96k -movflags +faststart local/fixture/fixture.mp4
ffmpeg -hide_banner -loglevel error -y -i local/fixture/fixture.mp4 -c copy -hls_time 4 -hls_list_size 0 -hls_segment_filename 'local/fixture/hls/segment%03d.ts' local/fixture/hls/master.m3u8
for id in film-1 film-2 series-1 episode-1; do
 ffmpeg -hide_banner -loglevel error -y -f lavfi -i 'testsrc2=size=320x480:rate=1:duration=1' -frames:v 1 "local/fixture/$id.jpg"
done

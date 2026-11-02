#!/usr/bin/env python3
"""Collect attribution from the exact resolved runtime POMs, with no credentials or network."""
from pathlib import Path
import xml.etree.ElementTree as E
root=Path(__file__).resolve().parent.parent
cache=Path.home()/'.gradle/caches/modules-2/files-2.1'
coordinates=(root/'app/build/reports/runtime-coordinates.txt').read_text().splitlines()
out=['# Third-party runtime dependencies','', 'Generated from the exact resolved release runtime graph and its cached Maven POMs. Original licenses apply to each component. Test-only dependencies, the build JDK and Gradle are not bundled with the app.','', '| Component | License declared by publisher | Source / license information |','| --- | --- | --- |']
for coord in coordinates:
 g,n,v=coord.split(':',2)
 if g=='AetherCinema':continue
 poms=list((cache/g/n/v).glob('*/*.pom'))
 if not poms:raise RuntimeError('Missing runtime POM: '+coord)
 tree=E.parse(poms[0]);ns={'m':'http://maven.apache.org/POM/4.0.0'}
 licenses=tree.findall('.//m:licenses/m:license',ns)
 names=[];links=[]
 for item in licenses:
  name=item.findtext('m:name',default='',namespaces=ns);url=item.findtext('m:url',default='',namespaces=ns)
  names.append(name);links.append(url)
 source=tree.findtext('m:scm/m:url',default='',namespaces=ns) or tree.findtext('m:url',default='',namespaces=ns)
 if not names: names=['No separate license element in this POM; see publisher source']
 refs=[x for x in [source]+links if x]
 out.append('| `'+coord+'` | '+'; '.join(names).replace('|','/')+' | '+' · '.join(f'[reference]({x})' for x in dict.fromkeys(refs))+' |')
out+=['','The GPL v2 upstream Jellyfin source/contributors and adopted paths are retained in `upstream/reference` and `docs/UPSTREAM.md`. AetherCinema uses the platform font and self-authored icon; no Apple design-kit font/image is bundled. MoonTV network protocol support is independently implemented and includes no downloaded web JavaScript.','', 'AndroidX graphics-path includes native path operations; see the AndroidX source and its Skia third-party license. The Apache2 full license and Skia BSD notice are bundled alongside this document.']
text='\n'.join(out)+'\n'
(root/'THIRD_PARTY_NOTICES.md').write_text(text)
(root/'app/src/main/assets/licenses/THIRD_PARTY_NOTICES.md').write_text(text)
print('Runtime components attributed:',len(coordinates))

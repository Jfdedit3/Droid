import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import vm from 'node:vm';
const source = readFileSync(new URL('../linuxfs/overlay/usr/local/share/droiddeck/windows-layout.js', import.meta.url), 'utf8');
function run(screens) {
  const created=[];
  const old=[{removed:false,remove(){this.removed=true;}}];
  const desktops=screens.map(screen=>({screen,keptWidget:'personal',writeConfig(k,v){this[k]=v;}}));
  function Panel(){this.widgets=[];this.addWidget=(type)=>{const w={type,writeConfig(k,v){this[k]=v;}};this.widgets.push(w);return w;};created.push(this);}
  vm.runInNewContext(source,{Panel,desktopsForActivity:()=>desktops,currentActivity:()=>'current',panels:()=>old});
  assert.equal(old[0].removed,true);
  for(const panel of created){
    assert.equal(panel.location,'bottom');assert.equal(panel.floating,false);assert.equal(panel.opacity,'opaque');
    assert.equal(panel.lengthMode,'fill');assert.equal(panel.widgets[0].type,'org.kde.plasma.kickoff');
    assert.ok(panel.widgets[1].launchers.includes('applications:droiddeck-brave.desktop'));
    assert.ok(panel.widgets[1].launchers.includes('applications:steamdeck-steam.desktop'));
  }
  for(const desktop of desktops){assert.equal(desktop.keptWidget,'personal');assert.match(desktop.Image,/windows-wallpaper.png$/);}
  return created;
}
assert.equal(run([0,0,1]).length,2);
assert.equal(run([]).length,1);
console.log('Windows-style layout: per-screen taskbars, application pins and desktop widget preservation passed');

package com.droiddeck.launcher.wayland
object WaylandCompositor {
 const val FG_ENGINE_LSFG=0; const val FG_ENGINE_DDFG=1
 var engine=-1; var model=-1; var preset=-1; var armed=false; var multiplier=0
 fun nativeSetFrameGenArmed(value:Boolean,mult:Int,target:Int){armed=value;multiplier=mult}
 fun nativeSetFrameGenEngine(kind:Int){engine=kind}
 fun nativeSetLsfgCachePath(path:String){}
 fun nativeSetFrameGenTuning(flow:Float,hz:Float){}
 fun nativeSetDdFgTuning(value:Int,perf:Int){model=value;preset=perf}
}

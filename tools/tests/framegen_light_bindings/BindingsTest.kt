package com.droiddeck.launcher.gpu
import android.content.Context
import org.junit.Assert.*
import org.junit.Test
import com.droiddeck.launcher.wayland.WaylandCompositor as Native
class BindingsTest {
 @Test fun savedLightModeIsAlwaysBoundedToTwoFrames() {
  val context=Context()
  FrameGen.set(context,FrameGen.Mode(FrameGen.ENGINE_LIGHT,4,120))
  assertEquals(FrameGen.Mode(FrameGen.ENGINE_LIGHT),FrameGen.mode(context))
  assertEquals(2,context.getSharedPreferences("frame_gen",0).getInt("multiplier",0))
  assertEquals(0,context.getSharedPreferences("frame_gen",0).getInt("target",-1))
 }
 @Test fun tamperedPreferencesCannotRequestFourTimesLightGeneration() {
  val context=Context()
  context.getSharedPreferences("frame_gen",0).edit().putString("engine",FrameGen.ENGINE_LIGHT).putInt("multiplier",4).putInt("target",120).apply()
  assertEquals(FrameGen.Mode(FrameGen.ENGINE_LIGHT),FrameGen.mode(context))
 }
 @Test fun lightStartsUnarmedAndRegularDdFgRestoresItsModel() {
  val context=Context()
  assertNull(FrameGen.apply(context,FrameGen.Mode(FrameGen.ENGINE_LIGHT),60f))
  assertTrue(LightFrameGenRuntime.running)
  assertFalse(Native.armed)
  assertEquals(3,Native.model)
  FrameGen.apply(context,FrameGen.Mode(FrameGen.ENGINE_DDFG,3),60f)
  assertFalse(LightFrameGenRuntime.running)
  assertEquals(4,Native.model)
  assertEquals(3,Native.multiplier)
  assertTrue(Native.armed)
 }
 @Test fun offStopsTheLightControllerWithoutChangingSavedPreference() {
  val context=Context()
  FrameGen.set(context,FrameGen.Mode(FrameGen.ENGINE_LIGHT))
  FrameGen.apply(context,60f)
  FrameGen.apply(context,FrameGen.Mode.OFF,60f)
  assertFalse(LightFrameGenRuntime.running)
  assertFalse(Native.armed)
  assertEquals(FrameGen.Mode(FrameGen.ENGINE_LIGHT),FrameGen.mode(context))
 }
 @Test fun perGameLightOverrideStaysBoundedWithoutReplacingGlobalChoice() {
  val context=Context()
  val global=FrameGen.Mode(FrameGen.ENGINE_DDFG,3)
  FrameGen.set(context,global)
  FrameGen.setForGame(context,"123",FrameGen.Mode(FrameGen.ENGINE_LIGHT,4,120))
  assertEquals(FrameGen.Mode(FrameGen.ENGINE_LIGHT),FrameGen.mode(context,"123"))
  assertEquals(global,FrameGen.mode(context))
  val saved=context.getSharedPreferences("frame_gen",0)
  assertEquals(2,saved.getInt("game.123.multiplier",0))
  assertEquals(0,saved.getInt("game.123.target",-1))
  FrameGen.clearGame(context,"123")
  assertEquals(global,FrameGen.mode(context,"123"))
 }
}

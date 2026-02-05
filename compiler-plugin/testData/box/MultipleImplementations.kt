// Test for multiple implementations of the same service
import java.util.ServiceLoader

interface Plugin

@AutoService(Plugin::class)
class PluginA : Plugin

@AutoService(Plugin::class)
class PluginB : Plugin

fun box(): String {
  val plugins = ServiceLoader.load(Plugin::class.java).toList()
  if (plugins.size != 2) return "FAIL: Expected 2 plugins, found ${plugins.size}"
  val types = plugins.map { it::class }.toSet()
  if (types != setOf(PluginA::class, PluginB::class)) return "FAIL: Wrong types"
  return "OK"
}

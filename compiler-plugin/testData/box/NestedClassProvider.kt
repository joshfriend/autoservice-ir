// Test nested class as service provider
import java.util.ServiceLoader

interface NestedService

object OuterClass {
  @AutoService(NestedService::class)
  class NestedProvider : NestedService
}

fun box(): String {
  val services = ServiceLoader.load(NestedService::class.java).toList()
  if (services.size != 1) return "FAIL: Expected 1 service, found ${services.size}"
  if (services.first()::class != OuterClass.NestedProvider::class) return "FAIL: Wrong type"
  return "OK"
}

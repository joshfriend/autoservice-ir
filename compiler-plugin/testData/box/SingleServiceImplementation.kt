// Simple test for single service implementation
import java.util.ServiceLoader

interface MyService

@AutoService(MyService::class)
class MyServiceImpl : MyService

fun box(): String {
  val services = ServiceLoader.load(MyService::class.java).toList()
  if (services.size != 1) return "FAIL: Expected 1 service, found ${services.size}"
  if (services.first()::class != MyServiceImpl::class) return "FAIL: Wrong type"
  return "OK"
}


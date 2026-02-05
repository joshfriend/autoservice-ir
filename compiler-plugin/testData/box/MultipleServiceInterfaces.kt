// Test implementing multiple service interfaces
import java.util.ServiceLoader

interface ServiceA
interface ServiceB

@AutoService(ServiceA::class, ServiceB::class)
class MultiServiceImpl : ServiceA, ServiceB

fun box(): String {
  val servicesA = ServiceLoader.load(ServiceA::class.java).toList()
  if (servicesA.size != 1) return "FAIL: Expected 1 ServiceA, found ${servicesA.size}"
  
  val servicesB = ServiceLoader.load(ServiceB::class.java).toList()
  if (servicesB.size != 1) return "FAIL: Expected 1 ServiceB, found ${servicesB.size}"
  
  if (servicesA.first()::class != MultiServiceImpl::class) return "FAIL: Wrong type for ServiceA"
  if (servicesB.first()::class != MultiServiceImpl::class) return "FAIL: Wrong type for ServiceB"
  
  return "OK"
}

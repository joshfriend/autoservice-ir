// Test inferring the service interface when not explicitly specified
import java.util.ServiceLoader

interface InferredService

@AutoService
class InferredServiceImpl : InferredService

fun box(): String {
  val services = ServiceLoader.load(InferredService::class.java).toList()
  if (services.size != 1) return "FAIL: Expected 1 service, found ${services.size}"
  if (services.first()::class != InferredServiceImpl::class) return "FAIL: Wrong type"
  return "OK"
}

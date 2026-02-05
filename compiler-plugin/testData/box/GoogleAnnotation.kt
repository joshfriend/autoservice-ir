// Test using Google's AutoService annotation
// NO_AUTO_IMPORT
import java.util.ServiceLoader
import com.google.auto.service.AutoService

interface GoogleService

@AutoService(GoogleService::class)
class GoogleServiceImpl : GoogleService

fun box(): String {
  val services = ServiceLoader.load(GoogleService::class.java).toList()
  if (services.size != 1) return "FAIL: Expected 1 service, found ${services.size}"
  if (services.first()::class != GoogleServiceImpl::class) return "FAIL: Wrong type"
  return "OK"
}

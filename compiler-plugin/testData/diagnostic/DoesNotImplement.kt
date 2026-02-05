// RENDER_DIAGNOSTICS_FULL_TEXT
// NO_AUTO_IMPORT

import com.fueledbycaffeine.autoservice.AutoService

interface ServiceA

interface ServiceB

@AutoService(ServiceB::class)
class <!AUTOSERVICE_DOES_NOT_IMPLEMENT!>WrongInterface<!> : ServiceA

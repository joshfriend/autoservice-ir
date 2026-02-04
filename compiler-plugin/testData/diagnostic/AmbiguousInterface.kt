// RENDER_DIAGNOSTICS_FULL_TEXT
// NO_AUTO_IMPORT

import com.fueledbycaffeine.autoservice.AutoService

interface ServiceA
interface ServiceB

@AutoService
class <!AUTOSERVICE_MISSING_SERVICE_INTERFACE!>AmbiguousService<!> : ServiceA, ServiceB

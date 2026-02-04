// RENDER_DIAGNOSTICS_FULL_TEXT
// NO_AUTO_IMPORT

import com.fueledbycaffeine.autoservice.AutoService

interface Service

@AutoService(Service::class)
interface <!AUTOSERVICE_WRONG_CLASS_KIND!>InterfaceService<!> : Service

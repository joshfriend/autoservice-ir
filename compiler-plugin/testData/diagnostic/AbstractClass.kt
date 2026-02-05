// RENDER_DIAGNOSTICS_FULL_TEXT
// NO_AUTO_IMPORT

import com.fueledbycaffeine.autoservice.AutoService

interface Service

@AutoService(Service::class)
<!AUTOSERVICE_ABSTRACT_CLASS!>abstract<!> class AbstractServiceImpl : Service


import 'package:flutter/material.dart';
import 'package:local_auth/local_auth.dart';
import 'package:webview_flutter/webview_flutter.dart';

import 'package:webview_flutter_android/webview_flutter_android.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const NutriVidaApp());
}

class NutriVidaApp extends StatelessWidget {
  const NutriVidaApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Nutri Vida',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        primaryColor: const Color(0xFF007BFF),
        colorScheme: ColorScheme.fromSeed(
          seedColor: const Color(0xFF007BFF),
        ),
        useMaterial3: true,
      ),
      home: const PantallaPrincipal(),
    );
  }
}

class PantallaPrincipal extends StatefulWidget {
  const PantallaPrincipal({super.key});

  @override
  State<PantallaPrincipal> createState() =>
      _PantallaPrincipalState();
}

class _PantallaPrincipalState extends State<PantallaPrincipal> {

  // Emulador Android:
  // http://10.0.2.2:8081/
  //
  // Para APK de produccion, utilizar
  // --dart-define=NUTRIVIDA_URL=https://...
  static const String urlInicial = String.fromEnvironment(
    'NUTRIVIDA_URL',
    defaultValue: 'http://10.0.2.2:8081/',
  );

  // Autenticacion del dispositivo.
  final LocalAuthentication autenticacion =
      LocalAuthentication();

  // Control de acceso a la WebView.
  bool desbloqueado = true;
  bool verificando = false;
  bool webIniciada = false;
  bool invitacionHuellaMostrada = false;

  String? mensajeBiometrico;

  late final WebViewController controller;

  int progreso = 0;
  String? errorCarga;

  @override
  void initState() {
    super.initState();

    // Preparar la WebView sin cargar la pagina.
    // Se cargara cuando la autenticacion
    // del dispositivo sea correcta.

    controller = WebViewController()
      ..setJavaScriptMode(JavaScriptMode.unrestricted)
      ..setBackgroundColor(Colors.white)
      ..setNavigationDelegate(
        NavigationDelegate(

          onProgress: (int progress) {
            if (!mounted) return;

            setState(() {
              progreso = progress;
            });
          },

          onPageStarted: (String url) {

  if (!mounted) return;

  debugPrint('NUTRIVIDA: Cargando: $url');

  setState(() {
    errorCarga = null;
  });

},

          
onPageFinished: (String url) async {

  if (!mounted) return;

  setState(() {
    progreso = 100;
  });

  final pagina = Uri.tryParse(url);

  if (pagina == null ||
      pagina.scheme != 'https' ||
      pagina.origin != Uri.parse(urlInicial).origin) {
    return;
  }

  // Mostrar el boton de huella en el login.
  if (pagina.path == '/login') {

    try {

      await controller.runJavaScript('''
        (() => {

          const acceso = document.getElementById(
            "acceso-huella-apk"
          );

          if (acceso) {
            acceso.style.display = "block";
          }

        })();
      ''');

    } catch (e) {

      debugPrint(
        'No se pudo mostrar el acceso biometrico: $e'
      );

    }
  }

  // Detectar el ingreso de cualquier rol.
  final paginasAutenticadas = [
    '/index',
    '/doctor',
    '/administrador',
  ];

  if (paginasAutenticadas.contains(pagina.path)) {

    await ofrecerActivacionHuella();

  }

},


          onWebResourceError: (WebResourceError error) async {

  if (error.isForMainFrame != true) return;
  if (!mounted) return;

  debugPrint('NUTRIVIDA: ERROR WEBVIEW');
  debugPrint('NUTRIVIDA: Codigo: ${error.errorCode}');
  debugPrint('NUTRIVIDA: Descripcion: ${error.description}');
  debugPrint('NUTRIVIDA: Tipo: ${error.errorType}');
  debugPrint('NUTRIVIDA: URL: ${error.url}');

  // Mostrar pantalla offline únicamente
  // cuando se detecte una desconexión.
  if (error.description.contains(
      'ERR_INTERNET_DISCONNECTED')) {

    try {

      await controller.loadFlutterAsset(
        'assets/offline.html',
      );

    } catch (e) {

      if (!mounted) return;

      setState(() {
        errorCarga = 'No hay conexion a Internet.';
      });

    }

    return;
  }

  // Mostrar el error real de navegación.
  setState(() {
    errorCarga =
        'Error ${error.errorCode}: ${error.description}';

    progreso = 100;
  });

},
        ),
      );

     // Iniciar la WebView directamente.
    webIniciada = true;
    iniciarWebViewConPasskeys();
  }

  // ======================================
  // AUTENTICACION BIOMETRICA
  // ======================================

  Future<void> desbloquearConBiometria() async {

    if (verificando || desbloqueado) {
      return;
    }

    setState(() {
      verificando = true;
      mensajeBiometrico = null;
    });

    try {

      final disponible =
          await autenticacion.isDeviceSupported();

      if (!mounted) return;

      if (!disponible) {
        setState(() {
          mensajeBiometrico =
              'Este dispositivo no admite autenticación '
              'local. Configura una huella o bloqueo '
              'seguro en Android.';
        });
        return;
      }

      final resultado = await autenticacion.authenticate(
        localizedReason:
            'Verifica tu identidad para abrir NUTRI VIDA',

        // Permite huella y, si corresponde,
        // PIN o patron del dispositivo.
        biometricOnly: false,

        // Maneja interrupciones mientras
        // esta abierto el dialogo de Android.
        persistAcrossBackgrounding: true,
      );

      if (!mounted) return;

      if (resultado) {

        setState(() {
          desbloqueado = true;
          mensajeBiometrico = null;
        });

        // Cargar la aplicacion una sola vez.
        if (!webIniciada) {

          webIniciada = true;

          try {
            await controller.loadRequest(
              Uri.parse(urlInicial),
            );
          } catch (e) {

            if (!mounted) return;

            setState(() {
              errorCarga =
                  'No se pudo abrir NUTRI VIDA.';
            });
          }
        }

      } else {

        setState(() {
          mensajeBiometrico =
              'No se completó la verificación. '
              'Inténtalo nuevamente.';
        });
      }

    } catch (e) {

      debugPrint('Error de autenticación: $e');

      if (!mounted) return;

      setState(() {
        mensajeBiometrico =
            'No se pudo realizar la verificación. '
            'Comprueba que tengas configurada '
            'una huella o un PIN en el celular.';
      });

    } finally {

      if (mounted) {
        setState(() {
          verificando = false;
        });
      }
    }
  }

  // ======================================
  // PANTALLA DE DESBLOQUEO
  // ======================================

  Widget pantallaBloqueo() {

    return Center(
      child: SingleChildScrollView(
        padding: const EdgeInsets.all(28),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [

            Container(
              padding: const EdgeInsets.all(25),
              decoration: BoxDecoration(
                color: const Color(0xFFE8F2FF),
                borderRadius: BorderRadius.circular(100),
              ),
              child: const Icon(
                Icons.fingerprint,
                size: 85,
                color: Color(0xFF007BFF),
              ),
            ),

            const SizedBox(height: 30),

            const Text(
              'NUTRI VIDA',
              style: TextStyle(
                fontSize: 28,
                fontWeight: FontWeight.bold,
                color: Color(0xFF007BFF),
              ),
            ),

            const SizedBox(height: 12),

            const Text(
              'Verificación de seguridad',
              textAlign: TextAlign.center,
              style: TextStyle(
                fontSize: 19,
                fontWeight: FontWeight.w600,
              ),
            ),

            const SizedBox(height: 15),

            const Text(
              'Confirma tu identidad con la huella '
              'digital o el bloqueo seguro de tu '
              'celular para continuar.',
              textAlign: TextAlign.center,
              style: TextStyle(
                fontSize: 15,
                color: Colors.black54,
              ),
            ),

            const SizedBox(height: 25),

            if (mensajeBiometrico != null)
              Padding(
                padding: const EdgeInsets.only(
                  bottom: 20,
                ),
                child: Text(
                  mensajeBiometrico!,
                  textAlign: TextAlign.center,
                  style: const TextStyle(
                    color: Colors.red,
                    fontSize: 14,
                  ),
                ),
              ),

            if (verificando)
              const Padding(
                padding: EdgeInsets.only(
                  bottom: 20,
                ),
                child: CircularProgressIndicator(),
              ),

            SizedBox(
              width: double.infinity,
              height: 52,
              child: ElevatedButton.icon(
                onPressed: verificando
                    ? null
                    : desbloquearConBiometria,

                icon: const Icon(
                  Icons.fingerprint,
                ),

                label: Text(
                  verificando
                      ? 'Verificando...'
                      : 'Desbloquear con huella o PIN',
                ),

                style: ElevatedButton.styleFrom(
                  backgroundColor:
                      const Color(0xFF007BFF),
                  foregroundColor: Colors.white,
                ),
              ),
            ),

            const SizedBox(height: 20),

            const Text(
              'La autenticación se realiza '
              'directamente en tu dispositivo.',
              textAlign: TextAlign.center,
              style: TextStyle(
                fontSize: 12,
                color: Colors.grey,
              ),
            ),
          ],
        ),
      ),
    );
  }

  // ======================================
  // NAVEGACION WEBVIEW
  // ======================================

  Future<void> volverAtras() async {

    if (!desbloqueado) return;

    if (await controller.canGoBack()) {
      await controller.goBack();
    }
  }

  Future<void> recargar() async {

    if (!desbloqueado) return;

    setState(() {
      errorCarga = null;
      progreso = 0;
    });

    await controller.loadRequest(
      Uri.parse(urlInicial),
    );
  }

  // ======================================
  // PANTALLA PRINCIPAL
  // ======================================

  @override
  Widget build(BuildContext context) {

    return Scaffold(
      appBar: AppBar(
        title: const Text(
          'NUTRI VIDA',
          style: TextStyle(
            fontWeight: FontWeight.bold,
            color: Colors.white,
          ),
        ),

        backgroundColor: const Color(0xFF007BFF),

        leading: desbloqueado
            ? IconButton(
                icon: const Icon(
                  Icons.arrow_back,
                  color: Colors.white,
                ),
                onPressed: volverAtras,
              )
            : null,

        actions: desbloqueado
            ? [
                IconButton(
                  icon: const Icon(
                    Icons.refresh,
                    color: Colors.white,
                  ),
                  onPressed: recargar,
                ),
              ]
            : [],
      ),

      body: SafeArea(
        child: desbloqueado
            ? Column(
                children: [

                  if (progreso < 100)
                    LinearProgressIndicator(
                      value: progreso / 100,
                      color: const Color(0xFF007BFF),
                    ),

                  Expanded(
                    child: errorCarga != null

                        ? Center(
                            child: Padding(
                              padding: const EdgeInsets.all(24),

                              child: Column(
                                mainAxisAlignment:
                                    MainAxisAlignment.center,

                                children: [

                                  const Icon(
                                    Icons.wifi_off,
                                    size: 65,
                                    color: Colors.grey,
                                  ),

                                  const SizedBox(height: 20),

                                  const Text(
                                    'No se pudo cargar Nutri Vida',
                                    style: TextStyle(
                                      fontSize: 20,
                                      fontWeight:
                                          FontWeight.bold,
                                    ),
                                    textAlign: TextAlign.center,
                                  ),

                                  const SizedBox(height: 10),

                                  Text(
                                    errorCarga!,
                                    textAlign: TextAlign.center,
                                  ),

                                  const SizedBox(height: 20),

                                  ElevatedButton(
                                    onPressed: recargar,
                                    child: const Text(
                                      'Reintentar',
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          )

                        : WebViewWidget(
                            controller: controller,
                          ),
                  ),
                ],
              )

            : pantallaBloqueo(),
      ),
    );
  }
  
Future<void> iniciarWebViewConPasskeys() async {

  final plataforma = controller.platform;

  if (plataforma is AndroidWebViewController) {

    try {

      final disponible =
          await plataforma.isWebViewFeatureSupported(
        WebViewFeatureType.webAuthentication,
      );

      debugPrint('NUTRIVIDA: WebAuthn disponible: $disponible');

      if (disponible) {

        await plataforma.setWebAuthenticationSupport(
          WebAuthenticationSupport.forApp,
        );

        debugPrint(
          'NUTRIVIDA: WebAuthn configurado en modo forApp',
        );

      } else {

        debugPrint(
          'NUTRIVIDA: WebAuthn no disponible',
        );

      }

    } catch (e) {

      debugPrint(
        'NUTRIVIDA: Error configurando WebAuthn: $e',
      );

    }

  }

  await controller.loadRequest(
    Uri.parse(urlInicial),
  );
}
Future<void> ofrecerActivacionHuella() async {

  if (!mounted || invitacionHuellaMostrada) {
    return;
  }

  invitacionHuellaMostrada = true;

  final activar = await showDialog<bool>(
    context: context,
    barrierDismissible: false,
    builder: (context) {

      return AlertDialog(

        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(25),
        ),

        icon: const Icon(
          Icons.fingerprint,
          size: 75,
          color: Color(0xFF007BFF),
        ),

        title: const Text(
          '¿Deseas activar tu huella digital?',
          textAlign: TextAlign.center,
        ),

        content: const Text(
          'Ingresa a NUTRI VIDA de forma rápida y '
          'segura sin tener que escribir tu contraseña.',
          textAlign: TextAlign.center,
        ),

        actionsAlignment: MainAxisAlignment.center,

        actions: [

          TextButton(
            onPressed: () {
              Navigator.of(context).pop(false);
            },
            child: const Text('Ahora no'),
          ),

          ElevatedButton(
            style: ElevatedButton.styleFrom(
              backgroundColor: const Color(0xFF007BFF),
              foregroundColor: Colors.white,
            ),
            onPressed: () {
              Navigator.of(context).pop(true);
            },
            child: const Text('Activar huella'),
          ),

        ],
      );
    },
  );

  if (activar == true && mounted) {
    await abrirRegistroHuella();
  }
}

Future<void> abrirRegistroHuella() async {

  final uri = Uri.parse(urlInicial);

  await controller.loadRequest(
    uri.resolve("/webauthn/register"),
  );
}

}

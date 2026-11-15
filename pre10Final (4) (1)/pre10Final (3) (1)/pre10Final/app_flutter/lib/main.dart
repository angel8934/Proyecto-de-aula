import 'package:flutter/material.dart';
import 'package:webview_flutter/webview_flutter.dart';

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
        colorScheme: ColorScheme.fromSeed(seedColor: const Color(0xFF007BFF)),
        useMaterial3: true,
      ),
      home: const PantallaPrincipal(),
    );
  }
}

class PantallaPrincipal extends StatefulWidget {
  const PantallaPrincipal({super.key});

  @override
  State<PantallaPrincipal> createState() => _PantallaPrincipalState();
}

class _PantallaPrincipalState extends State<PantallaPrincipal> {
  // Para el emulador Android:
  // http://10.0.2.2:8080/
  //
  // En producción usaremos la dirección HTTPS
  // del servidor Spring Boot.
  static const String urlInicial = String.fromEnvironment(
    'NUTRIVIDA_URL',
    defaultValue: 'http://10.0.2.2:8080/',
  );

  late final WebViewController controller;

  int progreso = 0;
  String? errorCarga;

  @override
  void initState() {
    super.initState();

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

            setState(() {
              errorCarga = null;
            });
          },

          onPageFinished: (String url) {
            if (!mounted) return;

            setState(() {
              progreso = 100;
            });
          },

          onWebResourceError: (WebResourceError error) async {
            if (error.isForMainFrame != true) return;
            if (!mounted) return;

            try {
              await controller.loadFlutterAsset('assets/offline.html');
            } catch (e) {
              if (!mounted) return;

              setState(() {
                errorCarga = 'No se pudo cargar la pantalla sin conexión.';
              });
            }
          },
        ),
      )
      ..loadRequest(Uri.parse(urlInicial));
  }

  Future<void> volverAtras() async {
    if (await controller.canGoBack()) {
      await controller.goBack();
    }
  }

  Future<void> recargar() async {
    setState(() {
      errorCarga = null;
      progreso = 0;
    });

    await controller.loadRequest(Uri.parse(urlInicial));
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text(
          'NUTRI VIDA',
          style: TextStyle(fontWeight: FontWeight.bold, color: Colors.white),
        ),

        backgroundColor: const Color(0xFF007BFF),

        leading: IconButton(
          icon: const Icon(Icons.arrow_back, color: Colors.white),
          onPressed: volverAtras,
        ),

        actions: [
          IconButton(
            icon: const Icon(Icons.refresh, color: Colors.white),
            onPressed: recargar,
          ),
        ],
      ),

      body: SafeArea(
        child: Column(
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
                          mainAxisAlignment: MainAxisAlignment.center,
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
                                fontWeight: FontWeight.bold,
                              ),
                              textAlign: TextAlign.center,
                            ),

                            const SizedBox(height: 10),

                            Text(errorCarga!, textAlign: TextAlign.center),

                            const SizedBox(height: 20),

                            ElevatedButton(
                              onPressed: () async {
                                setState(() {
                                  errorCarga = null;
                                });

                                await controller.loadRequest(
                                  Uri.parse(urlInicial),
                                );
                              },
                              child: const Text('Reintentar'),
                            ),
                          ],
                        ),
                      ),
                    )
                  : WebViewWidget(controller: controller),
            ),
          ],
        ),
      ),
    );
  }
}

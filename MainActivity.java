```java
package fr.ngplus.commandes;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

private static final int REQ_FICHIER = 1;
private static final int REQ_MICRO = 2;
private static final String FICHIER_ARTICLES = "articles.json";

private WebView web;
private SpeechRecognizer reco;
private ValueCallback<Uri[]> fichierCallback;
private boolean ecouteEnAttente = false;

@Override
protected void onCreate(Bundle savedInstanceState) {
super.onCreate(savedInstanceState);

web = new WebView(this);
setContentView(web);

WebSettings s = web.getSettings();
s.setJavaScriptEnabled(true);
s.setDomStorageEnabled(true);
s.setAllowFileAccess(true);
s.setMediaPlaybackRequiresUserGesture(false);

web.setWebViewClient(new WebViewClient());
web.setWebChromeClient(new WebChromeClient() {
@Override
public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> callback,
FileChooserParams params) {
if (fichierCallback != null) fichierCallback.onReceiveValue(null);
fichierCallback = callback;
Intent i = new Intent(Intent.ACTION_GET_CONTENT);
i.addCategory(Intent.CATEGORY_OPENABLE);
i.setType("*/*");
try {
startActivityForResult(Intent.createChooser(i, "Choisir article.xml"), REQ_FICHIER);
} catch (ActivityNotFoundException e) {
fichierCallback = null;
return false;
}
return true;
}
});

web.addJavascriptInterface(new Pont(), "AndroidVoix");
web.loadUrl("file:///android_asset/index.html");

if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_MICRO);
}
}

private class Pont {
@JavascriptInterface
public void ecouter() {
runOnUiThread(MainActivity.this::demarrerEcoute);
}

@JavascriptInterface
public void enregistrerCsv(String texte, String email) {
runOnUiThread(() -> sauverCsv(texte, email));
}

@JavascriptInterface
public boolean sauverArticles(String json) {
try (OutputStream os = new FileOutputStream(new File(getFilesDir(), FICHIER_ARTICLES))) {
os.write(json.getBytes(StandardCharsets.UTF_8));
return true;
} catch (Exception e) {
return false;
}
}

@JavascriptInterface
public String lireArticles() {
File f = new File(getFilesDir(), FICHIER_ARTICLES);
if (!f.exists()) return "";
try (InputStream is = new FileInputStream(f)) {
return new String(lireTout(is), StandardCharsets.UTF_8);
} catch (Exception e) {
return "";
}
}
}

private void demarrerEcoute() {
if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
ecouteEnAttente = true;
requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_MICRO);
return;
}
if (!SpeechRecognizer.isRecognitionAvailable(this)) {
erreurJs("reconnaissance vocale absente du telephone (installez ou activez l'application Google)");
return;
}
if (reco == null) {
reco = SpeechRecognizer.createSpeechRecognizer(this);
reco.setRecognitionListener(new Ecouteur());
}
Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fr-FR");
i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
reco.cancel();
reco.startListening(i);
}

private class Ecouteur implements RecognitionListener {
@Override public void onResults(Bundle b) {
ArrayList<String> l = b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
if (l != null && !l.isEmpty()) {
js("window.resultatVoix(" + JSONObject.quote(l.get(0)) + ")");
} else {
erreurJs("rien compris, reessayez");
}
}
@Override public void onError(int code) {
String m;
switch (code) {
case SpeechRecognizer.ERROR_NO_MATCH: m = "rien compris, reessayez"; break;
case SpeechRecognizer.ERROR_SPEECH_TIMEOUT: m = "aucune parole entendue"; break;
case SpeechRecognizer.ERROR_NETWORK:
case SpeechRecognizer.ERROR_NETWORK_TIMEOUT: m = "pas de reseau"; break;
case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS: m = "micro non autorise"; break;
case SpeechRecognizer.ERROR_RECOGNIZER_BUSY: m = "micro occupe, reessayez"; break;
case SpeechRecognizer.ERROR_AUDIO: m = "probleme de micro"; break;
default: m = "erreur " + code;
}
erreurJs(m);
}
@Override public void onReadyForSpeech(Bundle b) { js("window.pretVoix && window.pretVoix()"); }
@Override public void onBeginningOfSpeech() {}
@Override public void onRmsChanged(float v) {}
@Override public void onBufferReceived(byte[] b) {}
@Override public void onEndOfSpeech() {}
@Override public void onPartialResults(Bundle b) {}
@Override public void onEvent(int t, Bundle b) {}
}

private void js(String code) {
runOnUiThread(() -> web.evaluateJavascript(code, null));
}

private void erreurJs(String message) {
js("window.erreurVoix(" + JSONObject.quote(message) + ")");
}

// Enregistre le CSV dans Telechargements puis ouvre Gmail avec le fichier joint
private void sauverCsv(String texte, String email) {
String nom = "commandes_" + new SimpleDateFormat("yyyy-MM-dd_HH'h'mm", Locale.FRANCE).format(new Date()) + ".csv";
byte[] octets = texte.getBytes(StandardCharsets.UTF_8);
try {
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
ContentValues cv = new ContentValues();
cv.put(MediaStore.MediaColumns.DISPLAY_NAME, nom);
cv.put(MediaStore.MediaColumns.MIME_TYPE, "text/csv");
cv.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
if (uri == null) throw new Exception("creation impossible");
try (OutputStream os = getContentResolver().openOutputStream(uri)) {
os.write(octets);
}
Toast.makeText(this, "Enregistré dans Téléchargements : " + nom, Toast.LENGTH_LONG).show();
Intent partage = new Intent(Intent.ACTION_SEND);
partage.setType("text/csv");
partage.putExtra(Intent.EXTRA_STREAM, uri);
partage.putExtra(Intent.EXTRA_SUBJECT, nom);
partage.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
if (email != null && !email.isEmpty()) partage.putExtra(Intent.EXTRA_EMAIL, new String[]{email});
partage.setPackage("com.google.android.gm");
try {
startActivity(partage);
} catch (ActivityNotFoundException e) {
partage.setPackage(null);
startActivity(Intent.createChooser(partage, "Envoyer le fichier CSV"));
}
} else {
File dossier = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
File f = new File(dossier, nom);
try (OutputStream os = new FileOutputStream(f)) {
os.write(octets);
}
Toast.makeText(this, "Enregistré : " + f.getAbsolutePath(), Toast.LENGTH_LONG).show();
}
} catch (Exception e) {
Toast.makeText(this, "Erreur d'enregistrement : " + e.getMessage(), Toast.LENGTH_LONG).show();
}
}

@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
super.onActivityResult(requestCode, resultCode, data);
if (requestCode == REQ_FICHIER && fichierCallback != null) {
Uri[] r = null;
if (resultCode == RESULT_OK && data != null && data.getData() != null) {
r = new Uri[]{data.getData()};
}
fichierCallback.onReceiveValue(r);
fichierCallback = null;
}
}

@Override
public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
super.onRequestPermissionsResult(requestCode, permissions, results);
if (requestCode != REQ_MICRO) return;
boolean ok = results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED;
if (ecouteEnAttente) {
ecouteEnAttente = false;
if (ok) demarrerEcoute();
else erreurJs("micro refuse : autorisez-le dans Parametres > Applications > Commandes");
}
}

@Override
public void onBackPressed() {
web.evaluateJavascript(
"(function(){var p=document.getElementById('page2');" +
"if(p&&p.classList.contains('active')){document.getElementById('retour').click();return 1;}return 0;})()",
v -> { if (!"1".equals(v)) super.onBackPressed(); });
}

@Override
protected void onDestroy() {
if (reco != null) reco.destroy();
super.onDestroy();
}

private static byte[] lireTout(InputStream is) throws Exception {
ByteArrayOutputStream b = new ByteArrayOutputStream();
byte[] tampon = new byte[65536];
int n;
while ((n = is.read(tampon)) > 0) b.write(tampon, 0, n);
return b.toByteArray();

}
}

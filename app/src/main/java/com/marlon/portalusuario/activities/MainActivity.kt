package com.marlon.portalusuario.activities

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Icon
import android.net.ConnectivityManager
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ShareCompat.IntentBuilder
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import com.google.android.material.navigation.NavigationView
import com.marlon.portalusuario.PUNotifications.PUNotificationsActivity
import com.marlon.portalusuario.R
import com.marlon.portalusuario.ViewModel.PunViewModel
import com.marlon.portalusuario.banner.etecsa_scraping.Promo
import com.marlon.portalusuario.banner.etecsa_scraping.PromoSliderAdapter
import com.marlon.portalusuario.trafficbubble.BootReceiver
import com.marlon.portalusuario.trafficbubble.FloatingBubbleService
import com.marlon.portalusuario.databinding.ActivityMainBinding
import com.marlon.portalusuario.errores_log.JCLogging
import com.marlon.portalusuario.errores_log.LogFileViewerActivity
import com.marlon.portalusuario.huella.BiometricCallback
import com.marlon.portalusuario.huella.BiometricManager
import com.marlon.portalusuario.une.UneActivity
import com.marlon.portalusuario.util.SSLHelper
import com.marlon.portalusuario.util.Util
import com.marlon.portalusuario.view.fragments.CuentasFragment
import com.marlon.portalusuario.view.fragments.PaquetesFragment
import com.marlon.portalusuario.view.fragments.ServiciosFragment
import com.smarteist.autoimageslider.IndicatorView.animation.type.IndicatorAnimationType
import com.smarteist.autoimageslider.SliderAnimations
import com.smarteist.autoimageslider.SliderView
import cu.suitetecsa.nautanav.ui.ConnectivityFragment
import cu.uci.apklisupdate.ApklisUpdate
import cu.uci.apklisupdate.UpdateCallback
import cu.uci.apklisupdate.model.AppUpdateInfo
import cu.uci.apklisupdate.view.ApklisUpdateDialog
import dagger.hilt.android.AndroidEntryPoint
import io.github.suitetecsa.sdk.android.utils.extractShortNumber
import io.github.suitetecsa.sdk.android.utils.validateFormat
import org.jsoup.Connection
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), BiometricCallback {

    private lateinit var binding: ActivityMainBinding

    @Inject lateinit var connectivityFragment: ConnectivityFragment

    private var details: TextView? = null
    private var titleTextView: TextView? = null
    private var log: TextView? = null

    private var nameTV: TextView? = null
    private var mailTV: TextView? = null
    private var profileIV: ImageView? = null

    private var downloadApklis: Button? = null
    private var downloadPs: Button? = null
    private var remindMeLater: Button? = null
    private var progressBar: ProgressBar? = null
    private var errorLayout: LinearLayout? = null
    private var tryAgain: TextView? = null
    private var promoCache: List<Promo>? = null
    private var notificationBtn: FrameLayout? = null
    private var menu: ImageView? = null
    private var cartBadge: TextView? = null
    private var drawer: DrawerLayout? = null

    // VARS
    private var appName: String? = null

    // SETTINGS
    var settings: SharedPreferences? = null

    // LOGGING
    private var jcLogging: JCLogging? = null

    private var mBiometricManager: BiometricManager? = null

    private fun setFragment(fragment: Fragment?, title: String?) {
        supportFragmentManager
            .beginTransaction()
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            .replace(R.id.fragmentContainer, fragment!!)
            .commit()
        titleTextView!!.text = title
    }

    // preference dualSim
    private var simPreferences: SharedPreferences? = null
    private var simCard: String? = null

    @RequiresApi(api = Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Shorcuts
        shorcut()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        punViewModel = ViewModelProvider(this)[PunViewModel::class.java]
        appName = packageName
        // VALORES POR DEFECTO EN LAS PREFERENCIAS
        PreferenceManager.setDefaultValues(this, R.xml.root_preferences, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions()
        }

        simPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        context = this
        // drawer Layout
        drawer = findViewById(R.id.drawer_layout)
        // drawer Nav View
        navigationView = findViewById(R.id.nav_view)
        navigationView!!.setNavigationItemSelectedListener { item ->

            val i: Intent
            when (item.itemId) {
                R.id.micuenta -> setFragment(CuentasFragment(), "Mi Cuenta")
                R.id.services -> setFragment(ServiciosFragment<Any?>(), "Servicios")
                R.id.plans -> setFragment(PaquetesFragment(), "Planes")
                R.id.connectivity -> setFragment(connectivityFragment, "Conectividad")

                R.id.networkChange -> SetLTEModeDialog(context)
                R.id.une -> {
                    i = Intent(this@MainActivity, UneActivity::class.java)
                    startActivity(i)
                }

                R.id.errors_register -> startActivity(
                    Intent(
                        this@MainActivity,
                        LogFileViewerActivity::class.java
                    ).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                )

                R.id.feedback -> {
                    var debugInfo = "\n\n\n---"
                    debugInfo += "\nOS Version: " + System.getProperty("os.version") + " (" + Build.VERSION.INCREMENTAL + ")"
                    debugInfo += "\nAndroid API: " + Build.VERSION.SDK_INT
                    debugInfo += "\nModel (Device): " + Build.MODEL + " (" + Build.DEVICE + ")"
                    debugInfo += "\nManufacturer: " + Build.MANUFACTURER
                    debugInfo += "\n---"
                    val intent = Intent(
                        Intent.ACTION_SENDTO,
                        Uri.fromParts("mailto", context!!.getString(R.string.feedback_email), null)
                    )
                    intent.putExtra(
                        Intent.EXTRA_EMAIL,
                        context!!.getString(R.string.feedback_email)
                    )
                    intent.putExtra(
                        Intent.EXTRA_SUBJECT,
                        context!!.getString(R.string.feedback_subject)
                    )
                    intent.putExtra(Intent.EXTRA_TEXT, debugInfo)
                    startActivity(Intent.createChooser(intent, "Enviar Feedback usando..."))
                }

                R.id.telegram_channel -> {
                    val telegramUrl = ("https://t.me/portalusuario")
                    val telegramLaunch = Intent(Intent.ACTION_VIEW)
                    telegramLaunch.data = Uri.parse(telegramUrl)
                    startActivity(telegramLaunch)
                }

                R.id.facebook -> {
                    val facebookUrl = ("https://www.facebook.com/portalusuario")
                    val facebookLaunch = Intent(Intent.ACTION_VIEW)
                    facebookLaunch.data = Uri.parse(facebookUrl)
                    startActivity(facebookLaunch)
                }

                R.id.whatsapp -> {
                    val betaUrl = ("https://chat.whatsapp.com/HT6bKjpXHrN4FAyTAcy1Xn")
                    val betaLaunch = Intent(Intent.ACTION_VIEW)
                    betaLaunch.data = Uri.parse(betaUrl)
                    startActivity(betaLaunch)
                }

                R.id.betatesters -> {
                    val betaUrl = ("https://t.me/portalusuarioBT")
                    val betaLaunch = Intent(Intent.ACTION_VIEW)
                    betaLaunch.data = Uri.parse(betaUrl)
                    startActivity(betaLaunch)
                }

                R.id.invite -> {
                    inviteUser()
                }

                R.id.politicadeprivacidad -> {
                    i = Intent(this@MainActivity, PrivacyActivity::class.java)
                    startActivity(i)
                }

                R.id.settings -> {
                    i = Intent(this@MainActivity, SettingsActivity::class.java)
                    startActivity(i)
                }

                R.id.about -> {
                    i = Intent(this@MainActivity, AboutActivity::class.java)
                    startActivity(i)
                }

                R.id.donate -> {
                    i = Intent(this@MainActivity, DonationActivity::class.java)
                    startActivity(i)
                }
            }
            drawer!!.closeDrawer(GravityCompat.START)
            false
        }
        menu = findViewById(R.id.menu)
        menu!!.setOnClickListener { drawer!!.openDrawer(GravityCompat.START) }
        titleLayout = findViewById(R.id.titleLayout)
        titleTextView = findViewById(R.id.puTV)
        details = findViewById(R.id.details)
        log = findViewById(R.id.log)
        downloadApklis = findViewById(R.id.download_apklis)
        downloadApklis = findViewById(R.id.download_apklis)
        remindMeLater = findViewById(R.id.remind_me_later)
        nameTV = findViewById(R.id.textname)
        mailTV = findViewById(R.id.correotext)
        profileIV = findViewById(R.id.img_drawer_perfil)

        jcLogging = JCLogging(this)
        downloadApklis = findViewById(R.id.download_apklis)
        downloadPs = findViewById(R.id.download_ps)
        remindMeLater = findViewById(R.id.remind_me_later)
        settings = PreferenceManager.getDefaultSharedPreferences(this)

        // Burbuja de Trafico
        val bootReceiver = BootReceiver()
        JCLogging.message("Registering networkStateReceiver", null)
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(bootReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
        if (settings!!.getBoolean("show_traffic_speed_bubble", false)) {
            startFloatingBubbleService()
        }

        // Huella Seguridad
        if (settings!!.getBoolean("show_fingerprint", false)) {
            startFingerprint()
        }

        // Actualizacion de Aplicacion Apklis
        if (settings!!.getBoolean("start_checking_for_updates", true)) {
            ApklisUpdate.hasAppUpdate(
                this,
                object : UpdateCallback {
                    override fun onError(e: Throwable) {
                        // Not yet implemented
                    }

                    override fun onNewUpdate(appUpdateInfo: AppUpdateInfo) {
                        ApklisUpdateDialog(
                            this@MainActivity,
                            appUpdateInfo,
                            ContextCompat.getColor(this@MainActivity, R.color.colorPrimary)
                        ).show()
                    }

                    override fun onOldUpdate(appUpdateInfo: AppUpdateInfo) {
                        // Not yet implemented
                    }
                }
            )
        }

        // etecsa carousel
        carouselLayout = findViewById(R.id.carouselLayout)
        sliderView = findViewById(R.id.imageSlider)
        progressBar = findViewById(R.id.progressBar)
        errorLayout = findViewById(R.id.errorLayoutBanner)
        tryAgain = findViewById(R.id.try_again)
        tryAgain!!.paintFlags = tryAgain!!.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        tryAgain!!.setOnClickListener { loadPromo() }
        loadPromo()
        //
        // check if there are unseen notifications
        cartBadge = findViewById(R.id.cart_badge)
        setupBadge()
        notificationBtn = findViewById(R.id.notificationBtn)
        notificationBtn!!.setOnClickListener {
            val i = Intent(this@MainActivity, PUNotificationsActivity::class.java)
            startActivity(i)
            Toast.makeText(
                this@MainActivity,
                "Espera la nueva funcionalidad en próximas versiones 😉",
                Toast.LENGTH_SHORT
            ).show()
        }

        //
        setFragment(CuentasFragment(), "Servicios")
    }

    private fun setupBadge() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val count = sharedPreferences.getInt("notifications_count", 0)
        Log.e("UNSEE NOTIFICATIONS", count.toString())
        if (count == 0) {
            if (cartBadge!!.visibility != View.GONE) {
                cartBadge!!.visibility = View.GONE
            }
        } else {
            if (cartBadge!!.visibility != View.VISIBLE) {
                cartBadge!!.visibility = View.VISIBLE
            }
        }
        cartBadge!!.text = count.toString()
    }

    // Carrusel de ETECSA
    private fun loadPromo() {
        // hiding promos card view
        val settings = PreferenceManager.getDefaultSharedPreferences(this)
        val showEtecsaPromoCarousel = settings.getBoolean("show_etecsa_promo_carousel", true)
        if (showEtecsaPromoCarousel) {
            // mostrar progress && ocultar carrusel
            sliderView!!.visibility = View.INVISIBLE
            progressBar!!.visibility = View.VISIBLE
            // mostrar error
            errorLayout!!.visibility = View.INVISIBLE
            //
            if (promoCache != null && promoCache!!.isNotEmpty()) {
                updatePromoSlider(promoCache!!)
                return
            }
            //
            if (Util.isConnected(this@MainActivity)) {
                // llamada al metodo de scraping
                try {
                    SrapingPromo().execute()
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            } else {
                // ocultar progress && carrusel
                sliderView!!.visibility = View.INVISIBLE
                progressBar!!.visibility = View.INVISIBLE
                // mostrar error
                errorLayout!!.visibility = View.VISIBLE
            }
        }
        setCarouselVisibility(showEtecsaPromoCarousel)
    }

    // scrapear Promo de Etecsa
    inner class SrapingPromo : AsyncTask<Void?, Void?, List<Promo>>() {
        private var success = false

        // cuando se termine de ejecutar la accion doInBackground
        @Deprecated("Deprecated in Java")
        public override fun onPostExecute(promos: List<Promo>) {
            super.onPostExecute(promos)
            // adapter para cada item
            if (success) {
                try {
                    // ocultar error
                    errorLayout!!.visibility = View.INVISIBLE
                    // mostrar card view
                    sliderView!!.visibility = View.VISIBLE
                    //
                    updatePromoSlider(promos)
                    // ocultar progress bar
                    progressBar!!.visibility = View.INVISIBLE
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            } else {
                // ocultar progress bar
                progressBar!!.visibility = View.INVISIBLE
                // ocultar card view
                sliderView!!.visibility = View.INVISIBLE
                // mostrar error
                errorLayout!!.visibility = View.VISIBLE
            }
        }

        @Deprecated("Deprecated in Java")
        override fun doInBackground(vararg p0: Void?): List<Promo> {
            val promos: MutableList<Promo> = ArrayList()
            try {
                val response = SSLHelper.getConnection("https://www.etecsa.cu")
                    .userAgent(
                        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_6_8) AppleWebKit/534.30 (KHTML, like Gecko) Chrome/12.0.742.122 Safari/534.30"
                    )
                    .timeout(30000).ignoreContentType(true).method(
                        Connection.Method.GET
                    ).followRedirects(true).execute()
                if (response.statusCode() == 200) {
                    val parsed = response.parse()
                    // CAROUSEL
                    val carousel = parsed.select("div.carousel-inner").select("div.carousel-item")
                    for (i in carousel.indices) {
                        val items = carousel[i]
                        val mipromoContent = items.selectFirst("div.carousel-item")
                        val link = items.selectFirst("div.mipromocion-contenido")!!
                            .select("a").attr("href")
                        val toText = mipromoContent.toString()
                        val idx1 = toText.indexOf("<div style=\"background: url(\'")
                        val idx2 = toText.indexOf("\');")
                        var divStyle = toText.substring(idx1, idx2)
                        divStyle = divStyle.replace("<div style=\"background: url(\'", "")
                        //
                        val imageSvg = items.selectFirst("div.mipromocion-contenido")!!
                            .selectFirst("img")
                        val svg = imageSvg!!.attr("src")
                        //
                        promos.add(
                            Promo(
                                svg,
                                divStyle,
                                link
                            )
                        )
                    }
                    success = true
                } else {
                    success = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return promos
        }
    }

    fun updatePromoSlider(list: List<Promo>) {
        if (list.isNotEmpty()) {
            val adapter =
                PromoSliderAdapter(
                    this,
                    list as ArrayList<Promo>
                )
            sliderView!!.setSliderAdapter(adapter)
            // setting up el slider view
            sliderView!!.setIndicatorAnimation(IndicatorAnimationType.WORM)
            sliderView!!.setSliderTransformAnimation(SliderAnimations.CUBEINSCALINGTRANSFORMATION)
            sliderView!!.autoCycleDirection = SliderView.AUTO_CYCLE_DIRECTION_RIGHT
            sliderView!!.indicatorSelectedColor = Color.WHITE
            sliderView!!.indicatorUnselectedColor = Color.GRAY
            sliderView!!.scrollTimeInSec = 4
            sliderView!!.startAutoCycle()
        } else {
            carouselLayout!!.visibility = View.GONE
        }
    }

    private fun showMessage(c: Context?, message: String?) {
        Toast.makeText(c, message, Toast.LENGTH_SHORT).show()
    }

    // Huella de Seguridad
    @RequiresApi(api = Build.VERSION_CODES.M)
    fun startFingerprint() {
        val settings = PreferenceManager.getDefaultSharedPreferences(this)
        val showFingerprint = settings.getBoolean("show_fingerprint", false)
        if (showFingerprint) {
            mBiometricManager = BiometricManager.BiometricBuilder(this@MainActivity)
                .setTitle(getString(R.string.biometric_title))
                .setSubtitle(getString(R.string.biometric_subtitle))
                .setDescription(getString(R.string.biometric_description))
                .setNegativeButtonText(getString(R.string.biometric_negative_button_text))
                .build()

            // start authentication
            mBiometricManager!!.authenticate(this@MainActivity)
        }
    }

    // Invitar Usuario
    private fun inviteUser() {
        IntentBuilder(this)
            .setText(getString(R.string.invite_user) + packageName)
            .setType("text/plain")
            .setChooserTitle("Compartir:")
            .startChooser()
    }

    // Permisos Consedidos
    @RequiresApi(api = Build.VERSION_CODES.M)
    fun requestPermissions() {
        if ((
                (
                    (
                        (
                            ContextCompat.checkSelfPermission(
                                this,
                                Manifest.permission.CALL_PHONE
                            ) != PackageManager.PERMISSION_GRANTED
                            ) || ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.CAMERA
                        ) != PackageManager.PERMISSION_GRANTED
                        ) || ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.READ_CONTACTS
                    ) != PackageManager.PERMISSION_GRANTED
                    ) || ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
                ) || ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            startActivity(Intent(this, PermissionActivity::class.java))
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val errorMessage =
            "Cuidado... Faltan caracteres o su número seleccionado no es un número de telefonia móvil."
        for (fragment in supportFragmentManager.fragments) {
            fragment.onActivityResult(requestCode, resultCode, data)
        }
        if (requestCode == PICK_CONTACT_REQUEST) {
            if (resultCode == RESULT_OK) {
                val uri = data!!.data
                val cursor = contentResolver.query(uri!!, null, null, null, null)
                if (cursor!!.moveToFirst()) {
                    val numberColumn =
                        cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    val phoneNumber = cursor.getString(numberColumn)
                    validateFormat(phoneNumber)?.let {
                        extractShortNumber(it)?.let { shortNumber ->
                            ServiciosFragment.phoneNumber.setText(shortNumber)
                        } ?: run {
                            showMessage(this, errorMessage)
                        }
                    } ?: run {
                        showMessage(this, errorMessage)
                    }
                }
            }
        }

        // FLOATING BUBBLE SERVICE
        if (requestCode == 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
            startService(Intent(this, FloatingBubbleService::class.java))
        }
    }

    private fun shorcut() {
        if (Build.VERSION.SDK_INT >= 25) {
            val shortcutManager: ShortcutManager? =
                ContextCompat.getSystemService(this, ShortcutManager::class.java)
            val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:*222" + Uri.encode("#")))
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            intent.putExtra("com.android.phone.force.slot", true)
            intent.putExtra("Cdma_Supp", true)
            if (simCard == "0") {
                for (s in CuentasFragment.simSlotName) {
                    intent.putExtra(s, 0)
                    intent.putExtra("com.android.phone.extra.slot", 0)
                }
            } else if (simCard == "1") {
                for (s in CuentasFragment.simSlotName) {
                    intent.putExtra(s, 1)
                    intent.putExtra("com.android.phone.extra.slot", 1)
                }
            }
            val saldoShortcut = ShortcutInfo.Builder(this, "shortcut_saldo")
                .setShortLabel("Saldo")
                .setLongLabel("Saldo")
                .setIcon(Icon.createWithResource(this, R.drawable.saldosh))
                .setIntent(intent)
                .setRank(2)
                .build()
            // shrtcuts bonos
            val bonos = Intent(Intent.ACTION_CALL, Uri.parse("tel:*222*266" + Uri.encode("#")))
            bonos.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            bonos.putExtra("com.android.phone.force.slot", true)
            bonos.putExtra("Cdma_Supp", true)
            if (simCard == "0") {
                for (s in CuentasFragment.simSlotName) {
                    bonos.putExtra(s, 0)
                    bonos.putExtra("com.android.phone.extra.slot", 0)
                }
            } else if (simCard == "1") {
                for (s in CuentasFragment.simSlotName) {
                    bonos.putExtra(s, 1)
                    bonos.putExtra("com.android.phone.extra.slot", 1)
                }
            }
            val bonosShortcut = ShortcutInfo.Builder(this, "shortcut_bono")
                .setShortLabel("Bonos")
                .setLongLabel("Bonos")
                .setIcon(Icon.createWithResource(this, R.drawable.bolsash))
                .setIntent(bonos)
                .setRank(1)
                .build()
            // shrtcuts datos
            val datos = Intent(Intent.ACTION_CALL, Uri.parse("tel:*222*328" + Uri.encode("#")))
            datos.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            datos.putExtra("com.android.phone.force.slot", true)
            datos.putExtra("Cdma_Supp", true)
            if (simCard == "0") {
                for (s in CuentasFragment.simSlotName) {
                    datos.putExtra(s, 0)
                    datos.putExtra("com.android.phone.extra.slot", 0)
                }
            } else if (simCard == "1") {
                for (s in CuentasFragment.simSlotName) {
                    datos.putExtra(s, 1)
                    datos.putExtra("com.android.phone.extra.slot", 1)
                }
            }
            val datosShortcut = ShortcutInfo.Builder(this, "shortcut_datos")
                .setShortLabel("Datos")
                .setLongLabel("Datos")
                .setIcon(Icon.createWithResource(this, R.drawable.datossh))
                .setIntent(datos)
                .setRank(0)
                .build()
            if (shortcutManager != null) {
                shortcutManager.dynamicShortcuts =
                    listOf(saldoShortcut, bonosShortcut, datosShortcut)
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public override fun onResume() {
        super.onResume()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    fun startFloatingBubbleService() {
        if (FloatingBubbleService.isStarted) {
            return
        }
        if (Settings.canDrawOverlays(this)) {
            Log.i("CALLING ON MA", "STARTING SERVICE")
            stopService(Intent(applicationContext, FloatingBubbleService::class.java))
            startService(Intent(applicationContext, FloatingBubbleService::class.java))
        }
    }

    public override fun onPause() {
        super.onPause()
    }

    override fun onSdkVersionNotSupported() {
        Toast.makeText(
            applicationContext,
            getString(R.string.biometric_error_sdk_not_supported),
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onBiometricAuthenticationNotSupported() {
        Toast.makeText(
            applicationContext,
            getString(R.string.biometric_error_hardware_not_supported),
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onBiometricAuthenticationNotAvailable() {
        Toast.makeText(
            applicationContext,
            getString(R.string.biometric_error_fingerprint_not_available),
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onBiometricAuthenticationPermissionNotGranted() {
        Toast.makeText(
            applicationContext,
            getString(R.string.biometric_error_permission_not_granted),
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onBiometricAuthenticationInternalError(error: String) {
        Toast.makeText(applicationContext, error, Toast.LENGTH_LONG).show()
    }

    override fun onAuthenticationFailed() { /* no-op */ }
    override fun onAuthenticationCancelled() {
        mBiometricManager!!.cancelAuthentication()
        finish()
    }
    override fun onAuthenticationSuccessful() { /* no-op */ }
    override fun onAuthenticationHelp(helpCode: Int, helpString: CharSequence) { /* no-op */ }
    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
        mBiometricManager!!.cancelAuthentication()
        finish()
    }

    // Dialogo de Cambiar red
    inner class SetLTEModeDialog(context: Context?) {
        private val set4GBtn: Button

        init {
            val simDialog = Dialog(context!!)
            simDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            simDialog.setCancelable(true)
            simDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            simDialog.setContentView(R.layout.dialog_set_only_lte)
            set4GBtn = simDialog.findViewById(R.id.set_4g)
            set4GBtn.setOnClickListener { v: View? -> openHiddenMenu() }
            simDialog.show()
        }

        fun openHiddenMenu() {
            try {
                val intent = Intent("android.intent.action.MAIN")
                if (Build.VERSION.SDK_INT >= 30) {
                    intent.setClassName("com.android.phone", "com.android.phone.settings.RadioInfo")
                } else {
                    intent.setClassName("com.android.settings", "com.android.settings.RadioInfo")
                }
                startActivity(intent)
            } catch (unused: Exception) {
                Toast.makeText(
                    context,
                    "Su dispositivo no admite esta funcionalidad, lamentamos las molestias :(",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // Promo ETECSA
    companion object {
        private var context: Context? = null
        private var titleLayout: LinearLayout? = null

        // PROMO ETECSA CAROUSEL
        private var carouselLayout: RelativeLayout? = null
        private var sliderView: SliderView? = null
        var navigationView: NavigationView? = null
        private var punViewModel: PunViewModel? = null

        @JvmStatic
        fun insertNotification() {
            punViewModel!!.insertPUN(null)
        }

        const val PICK_CONTACT_REQUEST = 1

        @JvmStatic
        fun openLink(link: String?) {
            try {
                // JCLogging.message("Opening PROMO URL::url=" + link, null);
                val url = Uri.parse(link)
                val openUrl = Intent(Intent.ACTION_VIEW, url)
                context!!.startActivity(openUrl)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        @JvmStatic
        fun setCarouselVisibility(b: Boolean) {
            if (b) {
                carouselLayout!!.visibility = View.VISIBLE
            } else {
                carouselLayout!!.visibility = View.GONE
            }
        }
    }
}

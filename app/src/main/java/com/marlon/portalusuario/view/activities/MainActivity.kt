package com.marlon.portalusuario.view.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.net.ConnectivityManager
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.Settings
import android.telephony.CellInfo
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoWcdma
import android.telephony.PhoneStateListener
import android.telephony.ServiceState
import android.telephony.SignalStrength
import android.telephony.TelephonyManager
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.app.ShareCompat.IntentBuilder
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.navigation.NavigationView
import com.marlon.cz.mroczis.netmonster.core.factory.NetMonsterFactory.get
import com.marlon.cz.mroczis.netmonster.core.model.cell.CellCdma
import com.marlon.cz.mroczis.netmonster.core.model.cell.CellGsm
import com.marlon.cz.mroczis.netmonster.core.model.cell.CellLte
import com.marlon.cz.mroczis.netmonster.core.model.cell.CellNr
import com.marlon.cz.mroczis.netmonster.core.model.cell.CellTdscdma
import com.marlon.cz.mroczis.netmonster.core.model.cell.CellWcdma
import com.marlon.cz.mroczis.netmonster.core.model.cell.ICellProcessor
import com.marlon.portalusuario.PUNotifications.PUNotification
import com.marlon.portalusuario.PUNotifications.PUNotificationsActivity
import com.marlon.portalusuario.R
import com.marlon.portalusuario.Utils
import com.marlon.portalusuario.ViewModel.PunViewModel
import com.marlon.portalusuario.biometric.BiometricCallback
import com.marlon.portalusuario.biometric.BiometricManager
import com.marlon.portalusuario.databinding.ActivityMainBinding
import com.marlon.portalusuario.etecsa_scraping.Promo
import com.marlon.portalusuario.etecsa_scraping.PromoSliderAdapter
import com.marlon.portalusuario.firewall.ActivityMain
import com.marlon.portalusuario.floating_window.BootReceiver
import com.marlon.portalusuario.floating_window.FloatingBubbleService
import com.marlon.portalusuario.logging.JCLogging
import com.marlon.portalusuario.logging.LogFileViewerActivity
import com.marlon.portalusuario.senal.AppConfiguracionTool
import com.marlon.portalusuario.une.UneActivity
import com.marlon.portalusuario.util.Connectivity
import com.marlon.portalusuario.util.SSLHelper
import com.marlon.portalusuario.util.Util
import com.marlon.portalusuario.util.apklis.ApklisUtil
import com.marlon.portalusuario.view.fragments.BottomSheetDialog
import com.marlon.portalusuario.nauta.ui.ConnectivityFragment
import com.marlon.portalusuario.view.fragments.CuentasFragment
import com.marlon.portalusuario.view.fragments.HowToFragment
import com.marlon.portalusuario.view.fragments.PaquetesFragment
import com.marlon.portalusuario.view.fragments.ServiciosFragment
import com.marlon.portalusuario.view.fragments.connectivity.ConnectivityFragment
import com.smarteist.autoimageslider.IndicatorView.animation.type.IndicatorAnimationType
import com.smarteist.autoimageslider.SliderAnimations
import com.smarteist.autoimageslider.SliderView
import dagger.hilt.android.AndroidEntryPoint
import mobi.gspd.segmentedbarview.Segment
import mobi.gspd.segmentedbarview.SegmentedBarView
import mobi.gspd.segmentedbarview.SegmentedBarViewSideStyle
import org.jsoup.Connection
import java.util.Objects
import java.util.concurrent.RejectedExecutionException
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), BiometricCallback {
    private lateinit var binding: ActivityMainBinding
    @Inject lateinit var connectivityFragment: ConnectivityFragment

    private var details: TextView? = null
    private var titleTextView: TextView? = null
    private var log: TextView? = null

    // UI ELEMENTOS
    private var mBottomSheetLayout: LinearLayout? = null
    private var sheetBehavior: BottomSheetBehavior<*>? = null
    private var download_apklis: Button? = null
    private var download_ps: Button? = null
    private var remind_me_later: Button? = null
    private val bottomSheetDialog: BottomSheetDialog? = null
    private var progressBar: ProgressBar? = null
    private var errorLayout: LinearLayout? = null
    private var try_again: TextView? = null
    var promoCache: List<Promo>? = null
    private var notificationBtn: FrameLayout? = null
    private var menu: ImageView? = null
    private var cartBadge: TextView? = null
    private var drawer: DrawerLayout? = null

    // VARS
    private var APP_NAME: String? = null
    private val WAITING_TIME = 300
    private var update_info_already_showed = false

    // SETTINGS
    var settings: SharedPreferences? = null

    // LOGGING
    private var Logging: JCLogging? = null

    // APKLIS
    private var apklis: ApklisUtil? = null
    var mBiometricManager: BiometricManager? = null

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    var ResultCall = 1001
    fun setFragment(fragment: Fragment?, title: String?) {
        supportFragmentManager
            .beginTransaction()
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            .replace(R.id.fragmentContainer, fragment!!)
            .commit()
        titleTextView!!.text = title
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        punViewModel = ViewModelProvider(this)[PunViewModel::class.java]
        APP_NAME = packageName
        // VALORES POR DEFECTO EN LAS PREFERENCIAS
        PreferenceManager.setDefaultValues(this, R.xml.root_preferences, false)

        // check Donation's Time
        val donateMainOpen = AppConfiguracionTool.getDonateMainOpen(this)
        if ((donateMainOpen == 50 || donateMainOpen == 100) || donateMainOpen == 150) {
            makeDonation()
        }
        if (donateMainOpen != -1 && donateMainOpen <= 200) {
            AppConfiguracionTool.donateAddOneOpen(this)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions()
        }
        context = this
        // drawer Layout
        drawer = findViewById(R.id.drawer_layout)
        // drawer Nav View
        navigationView = findViewById(R.id.nav_view)
        navigationView!!.setNavigationItemSelectedListener(NavigationView.OnNavigationItemSelectedListener { item ->
            val i: Intent
            when (item.itemId) {
                R.id.micuenta -> setFragment(CuentasFragment(), "Mi Cuenta")
                R.id.services -> setFragment(ServiciosFragment<Any?>(), "Servicios")
                R.id.plans -> setFragment(PaquetesFragment(), "Planes")
                R.id.connectivity -> setFragment(connectivityFragment, "Conectividad")
                R.id.firewall -> {
                    i = Intent(this@MainActivity, ActivityMain::class.java)
                    startActivity(i)
                }

                R.id.networkChange -> SetLTEModeDialog(context)
                R.id.une -> {
                    i = Intent(this@MainActivity, UneActivity::class.java)
                    startActivity(i)
                }

                R.id.sim_info -> SimInfoDialog(context, this@MainActivity)
                R.id.tutos -> setFragment(HowToFragment(), "Información útil")
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
                    intent.putExtra(Intent.EXTRA_EMAIL, context!!.getString(R.string.feedback_email))
                    intent.putExtra(
                        Intent.EXTRA_SUBJECT,
                        context!!.getString(R.string.feedback_subject)
                    )
                    intent.putExtra(Intent.EXTRA_TEXT, debugInfo)
                    startActivity(Intent.createChooser(intent, "Enviar Feedback usando..."))
                }

                R.id.telegram_channel -> {
                    val telgramUrl = ("https://t.me/portalusuario")
                    val telegramLauch = Intent(Intent.ACTION_VIEW)
                    telegramLauch.data = Uri.parse(telgramUrl)
                    startActivity(telegramLauch)
                }

                R.id.facebook -> {
                    val facebookUrl = ("https://www.facebook.com/portalusuario")
                    val facebookLaunch = Intent(Intent.ACTION_VIEW)
                    facebookLaunch.data = Uri.parse(facebookUrl)
                    startActivity(facebookLaunch)
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

                R.id.settings -> {
                    i = Intent(this@MainActivity, SettingsActivity::class.java)
                    startActivity(i)
                }

                R.id.about -> {
                    i = Intent(this@MainActivity, AboutActivity::class.java)
                    startActivity(i)
                }

                R.id.donate -> {
                    i = Intent(this@MainActivity, Donacion::class.java)
                    startActivity(i)
                }
            }
            drawer!!.closeDrawer(GravityCompat.START)
            false
        })
        menu = findViewById(R.id.menu)
        menu!!.setOnClickListener(View.OnClickListener { drawer!!.openDrawer(GravityCompat.START) })
        titleLayout = findViewById(R.id.titleLayout)
        titleTextView = findViewById(R.id.puTV)
        details = findViewById(R.id.details)
        log = findViewById(R.id.log)
        download_apklis = findViewById(R.id.download_apklis)
        val details = findViewById<TextView>(R.id.details)
        val log = findViewById<TextView>(R.id.log)
        download_apklis = findViewById(R.id.download_apklis)
        remind_me_later = findViewById(R.id.remind_me_later)

        //
        Logging = JCLogging(this)
        download_apklis = findViewById(R.id.download_apklis)
        download_ps = findViewById(R.id.download_ps)
        remind_me_later = findViewById(R.id.remind_me_later)
        settings = PreferenceManager.getDefaultSharedPreferences(this)

        // FLOATING BUBBLE TRAFFIC SPEED
        val bootReceiver = BootReceiver()
        JCLogging.message("Registering networkStateReceiver", null)
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(bootReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
        if (settings!!.getBoolean("show_traffic_speed_bubble", false)) {
            startFloatingBubbleService()
        }

        // FINGERPRINT
        if (settings!!.getBoolean("show_fingerprint", false)) {
            startFingerprint()
        }
        val updateBtn = findViewById<Button>(R.id.updateBtn)
        mBottomSheetLayout = findViewById(R.id.bottom_sheet_update)
        sheetBehavior = BottomSheetBehavior.from(mBottomSheetLayout!!)
        sheetBehavior!!.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    updateBtn.visibility = View.GONE
                } else {
                    updateBtn.visibility = View.VISIBLE
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        })
        updateBtn.setOnClickListener { sheetBehavior!!.setState(BottomSheetBehavior.STATE_EXPANDED) }
        // DESCARGAR DE APKLIS
        download_apklis!!.setOnClickListener(View.OnClickListener {
            val URL = "https://www.apklis.cu/application/$APP_NAME"
            JCLogging.message("Opening Apklis URL::url=$URL", null)
            //Toast.makeText(this, URL, Toast.LENGTH_LONG);
            val url = Uri.parse(URL)
            val openUrl = Intent(Intent.ACTION_VIEW, url)
            startActivity(openUrl)
        })
        // DESCARGAR DE GOOGLE PLAY
        download_ps!!.setOnClickListener(View.OnClickListener {
            val URL = "https://play.google.com/store/apps/details?id=$APP_NAME"
            JCLogging.message("Opening PlayStore URL::url=$URL", null)
            //Toast.makeText(this, URL, Toast.LENGTH_LONG);
            val url = Uri.parse(URL)
            val openUrl = Intent(Intent.ACTION_VIEW, url)
            startActivity(openUrl)
        })
        // RECORDAR LUEGO
        remind_me_later!!.setOnClickListener(View.OnClickListener {
            update_info_already_showed = false
            startService(apklis, WAITING_TIME)
            sheetBehavior!!.setState(BottomSheetBehavior.STATE_HIDDEN)
        })
        sheetBehavior!!.state = BottomSheetBehavior.STATE_HIDDEN
        // CHECK FOR UPDATES
        val apklisUpdate: BroadcastReceiver
        if (settings!!.getBoolean("start_checking_for_updates", true)) {

            // BROADCAST
            apklisUpdate = object : BroadcastReceiver() {
                @SuppressLint("SetTextI18n")
                override fun onReceive(context: Context, intent: Intent) {
                    try {
                        val updateExist = intent.getBooleanExtra("update_exist", false)
                        Log.e("IS UPDATE", updateExist.toString())
                        if (updateExist) {
                            val version_name = intent.getStringExtra("version_name") /* Respuesta Del Método startLookingForUpdates() Valor De La Versión Name De La App
                                                                                           Si Existe Una Actualización */
                            val new_version_size = intent.getStringExtra("new_version_size")
                            val changelog = intent.getStringExtra("changelog")
                            JCLogging.message("On receive Update info", null)
                            var version_size = "?? MB"
                            if (new_version_size != null) {
                                version_size = new_version_size
                            }
                            // DETALLES de la NUEVA VERSION
                            if (changelog != null) {
                                log.text = changelog
                            } else {
                                log.text = "• Nada nuevo, todo igual :-)"
                            }
                            // NOMBRE DE LA VERSION Y TAMANNO
                            if (version_name != null && !version_name.isEmpty()) {
                                val v = "Versión $version_name • $version_size"
                                details.text = v
                            }
                            Log.e("Showing info", "True")
                            sheetBehavior!!.setState(BottomSheetBehavior.STATE_COLLAPSED)
                            JCLogging.message(
                                "Update data received succesfully::version_name=$version_name::version_size=$version_size",
                                null
                            )
                            return
                        }
                        sheetBehavior!!.setState(BottomSheetBehavior.STATE_HIDDEN)
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                        JCLogging.error(null, null, ex)
                    }
                }
            }
            /* Registro De Recibidores Para Manejar Existencia De Actualización U Obtención De Info Respectivamente */LocalBroadcastManager.getInstance(
                this
            ).registerReceiver(apklisUpdate, IntentFilter("apklis_update"))
            LocalBroadcastManager.getInstance(this)
                .registerReceiver(apklisUpdate, IntentFilter("apklis_app_info"))
            apklis = ApklisUtil(this, APP_NAME)
            startService(apklis, 0)

            // etecsa carousel
            carouselLayout = findViewById(R.id.carouselLayout)
            sliderView = findViewById(R.id.imageSlider)
            progressBar = findViewById(R.id.progressBar)
            errorLayout = findViewById(R.id.errorLayoutBanner)
            try_again = findViewById(R.id.try_again)
            try_again!!.paintFlags = try_again!!.paintFlags or Paint.UNDERLINE_TEXT_FLAG
            try_again!!.setOnClickListener(View.OnClickListener { loadPromo() })
            loadPromo()
            //
            // check if there are unseen notifications
            cartBadge = findViewById(R.id.cart_badge)
            setupBadge()
            notificationBtn = findViewById(R.id.notificationBtn)
            notificationBtn!!.setOnClickListener(View.OnClickListener {
                val i = Intent(this@MainActivity, PUNotificationsActivity::class.java)
                startActivity(i)
                Toast.makeText(
                    this@MainActivity,
                    "Espera la nueva funcionalidad en próximas versiones 😉",
                    Toast.LENGTH_SHORT
                ).show()
            })
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

    fun loadPromo() {
        // hiding promos card view
        val settings = PreferenceManager.getDefaultSharedPreferences(this)
        val show_etecsa_promo_carousel = settings.getBoolean("show_etecsa_promo_carousel", true)
        if (show_etecsa_promo_carousel) {
            // mostrar progress && ocultar carrusel
            sliderView!!.visibility = View.INVISIBLE
            progressBar!!.visibility = View.VISIBLE
            // mostrar error
            errorLayout!!.visibility = View.INVISIBLE
            //
            if (promoCache != null && !promoCache!!.isEmpty()) {
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
        setCarouselVisibility(show_etecsa_promo_carousel)
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
        override fun doInBackground(vararg p0: Void?): List<Promo>? {
            val promos: MutableList<Promo> = ArrayList()
            try {
                val response = SSLHelper.getConnection("https://www.etecsa.cu")
                    .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_6_8) AppleWebKit/534.30 (KHTML, like Gecko) Chrome/12.0.742.122 Safari/534.30")
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
                        promos.add(Promo(svg, divStyle, link))
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
        if (!list.isEmpty()) {
            val adapter = PromoSliderAdapter(this, list as ArrayList<Promo>)
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

    fun startService(apklis: ApklisUtil?, latency: Int) {
        val settings = PreferenceManager.getDefaultSharedPreferences(this)
        JCLogging.message(
            "Starting 'checking for updates' service::enabled=" + settings.getBoolean(
                "start_checking_for_updates",
                true
            )
                .toString() + "::latency=" + latency + "::update_info_already_showed=" + update_info_already_showed,
            null
        )
        if (!update_info_already_showed) {
            apklis!!.startLookingForUpdates(latency)
            update_info_already_showed = true
        }
    }

    private fun showMessage(c: Context?, _s: String?) {
        Toast.makeText(c, _s, Toast.LENGTH_SHORT).show()
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    fun startFingerprint() {
        val settings = PreferenceManager.getDefaultSharedPreferences(this)
        val show_fingerprint = settings.getBoolean("show_fingerprint", false)
        if (show_fingerprint) {
            mBiometricManager = BiometricManager.BiometricBuilder(this@MainActivity)
                .setTitle(getString(R.string.biometric_title))
                .setSubtitle(getString(R.string.biometric_subtitle))
                .setDescription(getString(R.string.biometric_description))
                .setNegativeButtonText(getString(R.string.biometric_negative_button_text))
                .build()

            //start authentication
            mBiometricManager!!.authenticate(this@MainActivity)
        }
    }

    private fun inviteUser() {
        IntentBuilder(this)
            .setText(getString(R.string.invite_user) + packageName)
            .setType("text/plain")
            .setChooserTitle("Compartir:")
            .startChooser()
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    fun requestPermissions() {
        if (((((ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CALL_PHONE
            ) != PackageManager.PERMISSION_GRANTED) || ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED) || ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED) || ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED) || ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            startActivity(Intent(this, PermissionActivity::class.java))
        }
    }

    var ca0: String? = null
    var ca1: String? = null
    var ca2: String? = null
    var ca3: String? = null
    var ca4: String? = null
    var ca5: String? = null
    var ca6: String? = null
    var ca7: String? = null
    var ca8: String? = null
    var ca9: String? = null
    var ca10: String? = null
    var ca11: String? = null
    var ca12: String? = null
    var ca13: String? = null
    var ca14: String? = null
    var ca15: String? = null
    var as0: String? = null
    var as1: String? = null
    var as2: String? = null
    var as3: String? = null
    var as4: String? = null
    var as12: String? = null
    var as13: String? = null
    var qwe = ""
    var union = ""
    var errorMessage = "Numero erroneo"
    var cuantos_caracteres = 0
    var error2 =
        "            Cuidado ..\n Faltan caracteres o su número seleccionado no es un número de telefonia móvil  "

    @RequiresApi(api = Build.VERSION_CODES.M)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        for (fragment in supportFragmentManager.fragments) {
            fragment.onActivityResult(requestCode, resultCode, data)
        }
        if (requestCode == PICK_CONTACT_REQUEST) {
            if (resultCode == RESULT_OK) {
                val uri = data!!.data
                val cursor = contentResolver.query(uri!!, null, null, null, null)
                if (cursor!!.moveToFirst()) {
                    val columnaNombre =
                        cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val columnaNumero =
                        cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    val nombre = cursor.getString(columnaNombre)
                    val numero = cursor.getString(columnaNumero)
                    union = ""
                    if (numero.length > 0) {
                        ca0 = "" + numero[0] + ""
                        qwe = ca0!!
                        if (((((qwe == "0") || qwe == "1" || qwe == "2") || qwe == "3" || qwe == "4" || qwe == "5" || qwe == "6") || qwe == "7" || qwe == "8") || qwe == "9") {
                            union += qwe
                        }
                        if (numero.length > 1) {
                            ca1 = "" + numero[1] + ""
                            qwe = ca1!!
                            if (((((qwe == "0") || qwe == "1" || qwe == "2" || qwe == "3" || qwe == "4" || qwe == "5") || qwe == "6") || qwe == "7" || qwe == "8") || qwe == "9") {
                                union += qwe
                            }
                            if (numero.length > 2) {
                                ca2 = "" + numero[2] + ""
                                qwe = ca2!!
                                if ((((((qwe == "0" || qwe == "1" || qwe == "2") || qwe == "3") || qwe == "4") || qwe == "5") || qwe == "6" || qwe == "7" || qwe == "8") || qwe == "9") {
                                    union += qwe
                                }
                                if (numero.length > 3) {
                                    ca3 = "" + numero[3] + ""
                                    qwe = ca3!!
                                    if ((((qwe == "0" || qwe == "1" || qwe == "2" || qwe == "3") || qwe == "4" || qwe == "5" || qwe == "6") || qwe == "7" || qwe == "8") || qwe == "9") {
                                        union += qwe
                                    }
                                    if (numero.length > 4) {
                                        ca4 = "" + numero[4] + ""
                                        qwe = ca4!!
                                        if ((((qwe == "0" || qwe == "1" || qwe == "2" || qwe == "3") || qwe == "4") || qwe == "5" || qwe == "6" || qwe == "7" || qwe == "8") || qwe == "9") {
                                            union += qwe
                                        }
                                        if (numero.length > 5) {
                                            ca5 = "" + numero[5] + ""
                                            qwe = ca5!!
                                            if ((((((qwe == "0") || qwe == "1" || qwe == "2" || qwe == "3") || qwe == "4") || qwe == "5") || qwe == "6" || qwe == "7") || qwe == "8" || qwe == "9") {
                                                union += qwe
                                            }
                                            if (numero.length > 6) {
                                                ca6 = "" + numero[6] + ""
                                                qwe = ca6!!
                                                if (((((qwe == "0") || qwe == "1") || qwe == "2" || qwe == "3") || qwe == "4" || qwe == "5") || qwe == "6" || qwe == "7" || qwe == "8" || qwe == "9") {
                                                    union += qwe
                                                }
                                                if (numero.length > 7) {
                                                    ca7 = "" + numero[7] + ""
                                                    qwe = ca7!!
                                                    if (((((((((qwe == "0") || qwe == "1") || qwe == "2") || qwe == "3") || qwe == "4") || qwe == "5") || qwe == "6" || qwe == "7") || qwe == "8") || qwe == "9") {
                                                        union += qwe
                                                    }
                                                    if (numero.length > 8) {
                                                        ca8 = "" + numero[8] + ""
                                                        qwe = ca8!!
                                                        if ((((((qwe == "0" || qwe == "1" || qwe == "2") || qwe == "3" || qwe == "4") || qwe == "5") || qwe == "6") || qwe == "7") || qwe == "8" || qwe == "9") {
                                                            union += qwe
                                                        }
                                                        if (numero.length > 9) {
                                                            ca9 = "" + numero[9] + ""
                                                            qwe = ca9!!
                                                            if (((((((qwe == "0" || qwe == "1") || qwe == "2" || qwe == "3") || qwe == "4" || qwe == "5") || qwe == "6") || qwe == "7") || qwe == "8") || qwe == "9") {
                                                                union += qwe
                                                            }
                                                            if (numero.length > 10) {
                                                                ca10 = "" + numero[10] + ""
                                                                qwe = ca10!!
                                                                if ((((((qwe == "0" || qwe == "1") || qwe == "2") || qwe == "3" || qwe == "4") || qwe == "5" || qwe == "6") || qwe == "7") || qwe == "8" || qwe == "9") {
                                                                    union += qwe
                                                                }
                                                                if (numero.length > 11) {
                                                                    ca11 = "" + numero[11] + ""
                                                                    qwe = ca11!!
                                                                    if (((((((qwe == "0" || qwe == "1") || qwe == "2" || qwe == "3" || qwe == "4") || qwe == "5") || qwe == "6") || qwe == "7") || qwe == "8") || qwe == "9") {
                                                                        union += qwe
                                                                    }
                                                                    if (numero.length > 12) {
                                                                        ca12 = "" + numero[12] + ""
                                                                        qwe = ca12!!
                                                                        if (((((((qwe == "0" || qwe == "1" || qwe == "2") || qwe == "3") || qwe == "4") || qwe == "5") || qwe == "6" || qwe == "7") || qwe == "8") || qwe == "9") {
                                                                            union += qwe
                                                                        }
                                                                        if (numero.length > 13) {
                                                                            ca13 =
                                                                                "" + numero[13] + ""
                                                                            qwe = ca13!!
                                                                            if ((((((qwe == "0") || qwe == "1" || qwe == "2") || qwe == "3" || qwe == "4") || qwe == "5" || qwe == "6" || qwe == "7") || qwe == "8") || qwe == "9") {
                                                                                union += qwe
                                                                            }
                                                                            if (numero.length > 14) {
                                                                                ca14 =
                                                                                    "" + numero[14] + ""
                                                                                qwe = ca14!!
                                                                                if ((((((qwe == "0") || qwe == "1") || qwe == "2" || qwe == "3") || qwe == "4") || qwe == "5") || qwe == "6" || qwe == "7" || qwe == "8" || qwe == "9") {
                                                                                    union += qwe
                                                                                }
                                                                                if (numero.length > 15) {
                                                                                    ca15 =
                                                                                        "" + numero[15] + ""
                                                                                    qwe = ca15!!
                                                                                    if (((((qwe == "0" || qwe == "1" || qwe == "2") || qwe == "3" || qwe == "4" || qwe == "5") || qwe == "6" || qwe == "7") || qwe == "8") || qwe == "9") {
                                                                                        union += qwe
                                                                                    }
                                                                                }
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (union.length == 8) {
                        val ewq = "" + union[0] + ""
                        if (ewq == "5") {
                            ServiciosFragment.phoneNumber.setText(union)
                        } else {
                            showMessage(this, errorMessage)
                        }
                    } else {
                        if (union.length < 15) {
                            cuantos_caracteres = union.length
                            if (cuantos_caracteres == 14) {
                                as0 = "" + union[0] + ""
                                as1 = "" + union[1] + ""
                                as2 = "" + union[2] + ""
                                as3 = "" + union[3] + ""
                                as4 = "" + union[4] + ""
                                as12 = "" + union[12] + ""
                                as13 = "" + union[13] + ""
                                if (((((as0 == "9") && as1 == "9" && as2 == "5") && as3 == "3") && as4 == "5" && as12 == "9") && as13 == "9") {
                                    val nuu =
                                        "" + union[4] + union[5] + union[6] + union[7] + union[8] + union[9] + union[10] + union[11] + ""
                                    ServiciosFragment.phoneNumber.setText(nuu)
                                } else {
                                    showMessage(this, errorMessage)
                                }
                            } else {
                                cuantos_caracteres = union.length
                                if (cuantos_caracteres == 10) {
                                    as0 = "" + union[0] + ""
                                    as1 = "" + union[1] + ""
                                    as2 = "" + union[2] + ""
                                    if ((as0 == "5" && as1 == "3") || (as0 == "9" && as1 == "9")) {
                                        if (as2 == "5") {
                                            val nuu =
                                                "" + union[2] + union[3] + union[4] + union[5] + union[6] + union[7] + union[8] + union[9] + ""
                                            ServiciosFragment.phoneNumber.setText(nuu)
                                        } else {
                                            showMessage(this, errorMessage)
                                        }
                                    } else {
                                        showMessage(this, errorMessage)
                                    }
                                } else {
                                    if (cuantos_caracteres < 8) {
                                        ServiciosFragment.phoneNumber.setText(union)
                                        showMessage(this, error2)
                                    } else {
                                        showMessage(this, errorMessage)
                                    }
                                }
                            }
                        } else {
                            showMessage(this, errorMessage)
                        }
                    }


                    //textnombre.setText(nombre);
                    // editnumero.setText(numero);
                }
            }
        }
        // FLOATING BUBBLE SERVICE
        if (requestCode == 0) {
            if (!Settings.canDrawOverlays(this)) {
            } else {
                startService(Intent(this, FloatingBubbleService::class.java))
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
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
        if (settings!!.getBoolean("show_traffic_speed_bubble", false)) {
            //unregisterReceiver(networkStateReceiver);
        }
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

    override fun onAuthenticationFailed() {}
    override fun onAuthenticationCancelled() {
        mBiometricManager!!.cancelAuthentication()
        finish()
    }

    override fun finish() {
        super.finish()
    }

    override fun onAuthenticationSuccessful() {}
    override fun onAuthenticationHelp(helpCode: Int, helpString: CharSequence) {}
    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
        mBiometricManager!!.cancelAuthentication()
        finish()
    }

    inner class DonationDialog(
        context: Context
    ) {
        init {
            val donationDialog = Dialog(context)
            donationDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            donationDialog.setCancelable(true)
            donationDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            donationDialog.setContentView(R.layout.alert_dialog)
            val mount = donationDialog.findViewById<EditText>(R.id.dialog_donation_mount_et)
            val key = donationDialog.findViewById<EditText>(R.id.dialog_donation_key_et)
            val bpaLayout = donationDialog.findViewById<LinearLayout>(R.id.bpa_layout)
            val bpaTransfer = donationDialog.findViewById<TextView>(R.id.bpa_transfer)
            bpaTransfer.setOnClickListener(View.OnClickListener {
                bpaTransfer.visibility = View.GONE
                bpaLayout.visibility = View.VISIBLE
            })
            val imageView = donationDialog.findViewById<ImageView>(R.id.close)
            imageView.setOnClickListener(object : View.OnClickListener {
                override fun onClick(view: View) {
                    bpaLayout.visibility = View.GONE
                    bpaTransfer.visibility = View.VISIBLE
                }
            })
            val send = donationDialog.findViewById<ImageView>(R.id.dialog_send_donation_btn)
            send.setOnClickListener { v: View? ->
                val donationMount: String = mount.getText().toString().trim { it <= ' ' }
                if ((donationMount == "")) {
                    Toast.makeText(context, "Todos los campos son obligatorios", Toast.LENGTH_SHORT)
                        .show()
                    return@setOnClickListener
                }
                val donationKey: String = key.getText().toString().trim { it <= ' ' }
                if ((donationKey == "")) {
                    Toast.makeText(context, "Todos los campos son obligatorios", Toast.LENGTH_SHORT)
                        .show()
                    return@setOnClickListener
                }
                val ussd: String = "*234*1*54871663*$donationKey*$donationMount%23"
                val r: Intent = Intent()
                r.action = Intent.ACTION_CALL
                r.data = Uri.parse("tel:$ussd")
                if (Build.VERSION.SDK_INT >= 23) {
                    if (context.checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_DENIED) {
                        requestPermissions(arrayOf(Manifest.permission.CALL_PHONE), 1000)
                    } else {
                        startActivity(r)
                    }
                } else {
                    startActivity(r)
                }
                donationDialog.dismiss()
            }
            donationDialog.show()
        }
    }

    private fun makeDonation() {
        val builder = AlertDialog.Builder(this@MainActivity)
        builder.setTitle(R.string.main_dialog_donate_title)
            .setMessage(R.string.main_action_about_donar_text)
            .setPositiveButton(
                resources.getText(R.string.main_action_about_donar_button),
                donateButtonListener()
            )
            .setNegativeButton(resources.getText(R.string.cancelar), cancelButtonListener())
        val create = builder.create()
        create.setCancelable(false)
        create.show()
    }

    inner class cancelButtonListener internal constructor() : DialogInterface.OnClickListener {
        override fun onClick(dialogInterface: DialogInterface, i: Int) {
            dialogInterface.dismiss()
        }
    }

    inner class donateButtonListener internal constructor() : DialogInterface.OnClickListener {
        override fun onClick(dialogInterface: DialogInterface, i: Int) {
            DonationDialog(this@MainActivity)
            dialogInterface.dismiss()
        }
    }

    inner class SimInfoDialog(context: Context?, activity: Activity) {
        val serviceState = ServiceState()
        private var listener: PhoneStateListener? = null
        var listener2: PhoneStateListener? = null
        private val networkClass: TextView
        private val telephonyManager: TelephonyManager
        private val tvSignal: TextView
        private val Utils: Utils
        private val bv: SegmentedBarView
        private val Conn: Connectivity
        private val activity: Activity

        init {
            val simDialog = Dialog(context!!)
            this.activity = activity
            simDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            simDialog.setCancelable(true)
            simDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            simDialog.setContentView(R.layout.dialog_info_sim)
            bv = simDialog.findViewById(R.id.bar_view)
            tvSignal = simDialog.findViewById(R.id.tvSignal)
            networkClass = simDialog.findViewById(R.id.tvTipoRedM)
            Utils = Utils(applicationContext)
            Conn = Connectivity()
            telephonyManager = context.getSystemService(TELEPHONY_SERVICE) as TelephonyManager
            restartSignalForceSegment()
            obtainSignalType()
            restartSignalSegment()
            simDialog.show()
        }

        private fun restartSignalSegment() {
            val arrayList: ArrayList<Segment> = ArrayList()
            if (networkClass.text.toString().contains("4G")) {
                arrayList.add(Segment(-100.0f, -95.0f, "Mala", Color.parseColor("#ffd50000")))
                arrayList.add(Segment(-90.0f, -85.0f, "", Color.parseColor("#ffd50000")))
                arrayList.add(Segment(-80.0f, -75.0f, "Buena", Color.parseColor("#ffffd600")))
                arrayList.add(Segment(-70.0f, -65.0f, "", Color.parseColor("#8CC63E")))
                arrayList.add(Segment(-60.0f, -0.0f, "Perfecta", Color.parseColor("#8CC63E")))
            } else if (networkClass.text.toString().contains("3G")) {
                arrayList.add(Segment(-100.0f, -95.0f, "Mala", Color.parseColor("#ffd50000")))
                arrayList.add(Segment(-90.0f, -85.0f, "", Color.parseColor("#ffd50000")))
                arrayList.add(Segment(-80.0f, -75.0f, "Buena", Color.parseColor("#ffffd600")))
                arrayList.add(Segment(-70.0f, -65.0f, "", Color.parseColor("#8CC63E")))
                arrayList.add(Segment(-60.0f, -0.0f, "Perfecta", Color.parseColor("#8CC63E")))
            } else {
                arrayList.add(Segment(-100.0f, -95.0f, "Mala", Color.parseColor("#ffd50000")))
                arrayList.add(Segment(-90.0f, -85.0f, "", Color.parseColor("#ffd50000")))
                arrayList.add(Segment(-80.0f, -75.0f, "Buena", Color.parseColor("#ffffd600")))
                arrayList.add(Segment(-70.0f, -65.0f, "", Color.parseColor("#8CC63E")))
                arrayList.add(Segment(-60.0f, -0.0f, "Perfecta", Color.parseColor("#8CC63E")))
            }
            bv.setSegments(arrayList)
            bv.setUnit("dBm")
            bv.setShowDescriptionText(true)
            bv.setSideStyle(SegmentedBarViewSideStyle.ROUNDED)
            bv.setDescriptionTextColor(R.color.colorDes)
        }

        private fun restartSignalForceSegment() {
            @SuppressLint("WrongConstant") val telephonyManager2 =
                getSystemService("phone") as TelephonyManager
            if (Build.VERSION.SDK_INT >= 28 && !Utils.PermissionUELocationGPS(
                    applicationContext
                )
            ) {
                Toast.makeText(
                    applicationContext,
                    "Debe habilitar el permiso de ubicación",
                    Toast.LENGTH_SHORT
                ).show()
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(
                        "android.permission.ACCESS_COARSE_LOCATION",
                        "android.permission.ACCESS_FINE_LOCATION"
                    ),
                    1
                )
            }
            listener = object : PhoneStateListener() {
                @Deprecated("Deprecated in Java")
                @SuppressLint("WrongConstant")
                override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
                    val list: List<CellInfo>?
                    var valueOf: String? = null
                    super.onSignalStrengthsChanged(signalStrength)
                    val sb = StringBuilder()
                    val sb2 = StringBuilder()
                    if (!Utils.PermissionUELocation(context)) {
                        list = null
                    } else if (Build.VERSION.SDK_INT < 23 || context!!.checkSelfPermission("android.permission.ACCESS_COARSE_LOCATION") == 0) {
                        list = telephonyManager2.allCellInfo
                    } else {
                        return
                    }
                    if (Build.VERSION.SDK_INT < 23) {
                        try {
                            GetSignal(context!!).executeOnExecutor(
                                AsyncTask.THREAD_POOL_EXECUTOR,
                                *arrayOfNulls<Void>(0)
                            )
                        } catch (e: RejectedExecutionException) {
                            e.printStackTrace()
                        }
                    } else if ((context!!.checkSelfPermission("android.permission.ACCESS_FINE_LOCATION") == 0) && (context!!.checkSelfPermission(
                            "android.permission.ACCESS_COARSE_LOCATION"
                        ) == 0) && (context!!.checkSelfPermission("android.permission.READ_PHONE_STATE") == 0)
                    ) {
                        try {
                            GetSignal(context!!).executeOnExecutor(
                                AsyncTask.THREAD_POOL_EXECUTOR,
                                *arrayOfNulls<Void>(0)
                            )
                        } catch (e2: RejectedExecutionException) {
                            e2.printStackTrace()
                        }
                    } else {
                    }
                    var str: String? = "0"
                    if (list != null) {
                        Log.d("cellInfos", list.toString())
                        for (i in list.indices) {
                            if (list[i] is CellInfoWcdma) {
                                Log.d("cellInfos.get(i)", list[i].toString())
                                val cellInfoWcdma = list[i] as CellInfoWcdma
                                val cellSignalStrength = cellInfoWcdma.cellSignalStrength
                                Log.d("cellInfoWcdma", cellSignalStrength.dbm.toString())
                                valueOf = cellSignalStrength.dbm.toString()
                                if (list[i].isRegistered) {
                                    sb.append("\nCelda: ")
                                    sb.append(cellInfoWcdma.cellIdentity.cid)
                                    sb.append(" Red: 3G ")
                                    sb.append(" dBm: ")
                                    sb.append(valueOf)
                                } else {
                                    sb2.append("\nCelda: ")
                                    sb2.append(cellInfoWcdma.cellIdentity.cid)
                                    sb2.append(" Red: 3G ")
                                    sb2.append(" dBm: ")
                                    sb2.append(valueOf)
                                }
                            } else if (list[i] is CellInfoGsm) {
                                Log.d("cellInfos.get(i)", list[i].toString())
                                val cellInfoGsm = list[i] as CellInfoGsm
                                val cellSignalStrength2 = cellInfoGsm.cellSignalStrength
                                Log.d("cellInfoGsm", cellSignalStrength2.dbm.toString())
                                valueOf = cellSignalStrength2.dbm.toString()
                                if (list[i].isRegistered) {
                                    sb.append("\nCelda: ")
                                    sb.append(cellInfoGsm.cellIdentity.cid)
                                    sb.append(" Red: 2G ")
                                    sb.append(" dBm: ")
                                    sb.append(valueOf)
                                } else {
                                    sb2.append("\nCelda: ")
                                    sb2.append(cellInfoGsm.cellIdentity.cid)
                                    sb2.append(" Red: 2G ")
                                    sb2.append(" dBm: ")
                                    sb2.append(valueOf)
                                }
                            } else if (list[i] is CellInfoLte) {
                                Log.d("cellInfos.get(i)", list[i].toString())
                                val cellInfoLte = list[i] as CellInfoLte
                                val cellSignalStrength3 = cellInfoLte.cellSignalStrength
                                Log.d("cellInfoLte", cellSignalStrength3.dbm.toString())
                                valueOf = cellSignalStrength3.dbm.toString()
                                if (list[i].isRegistered) {
                                    sb.append("\nCelda: ")
                                    sb.append(cellInfoLte.cellIdentity.ci)
                                    sb.append(" Red: 4G ")
                                    sb.append(" dBm: ")
                                    sb.append(valueOf)
                                } else {
                                    sb2.append("\nCelda: ")
                                    sb2.append(cellInfoLte.cellIdentity.ci)
                                    sb2.append(" Red: 4G ")
                                    sb2.append(" dBm: ")
                                    sb2.append(valueOf)
                                }
                            }
                            str = valueOf
                        }
                        Log.d("RadioN:", sb.toString())
                        try {
                            if (sb.toString() != "") {
                                Log.d("RadioN2:", sb2.toString())
                            }
                        } catch (e3: Exception) {
                            e3.printStackTrace()
                        }
                    }
                    Log.v("Signal Strenght:", (str)!!)
                }
            }
        }

        @SuppressLint("WrongConstant")
        private fun obtainSignalType() {
            networkClass.text = Conn.getNetworkClass(context)
            if (Utils.PermissionUELocation(applicationContext)) {
                try {
                    if ((Build.VERSION.SDK_INT < 23) || applicationContext.checkSelfPermission("android.permission.ACCESS_FINE_LOCATION") == 0 || applicationContext.checkSelfPermission(
                            "android.permission.ACCESS_COARSE_LOCATION"
                        ) == 0
                    ) {
                        telephonyManager.listen(listener, 256)
                    }
                } catch (e: ClassCastException) {
                    e.printStackTrace()
                }
            }
        }

        inner class GetSignal(var context: Context) : AsyncTask<Void?, Void?, Void?>() {
            var banda = ""
            var rssi = 0.0f
            @Deprecated("Deprecated in Java")
            override fun doInBackground(vararg p0: Void?): Void? {
                @SuppressLint("MissingPermission") val cells = get(this.context).getCells()
                val r0: ICellProcessor<Unit> = object : ICellProcessor<Unit> {
                    override fun processCdma(cell: CellCdma) {
                        try {
                            val num = (Objects.requireNonNull(
                                cell.band!!.number
                            ) as Int).toString()
                            val name = cell.band.name
                            banda = "B$num - $name"
                            rssi = java.lang.Float.valueOf(
                                cell.signal.cdmaRssi!!.toInt().toFloat()
                            )
                        } catch (e: NullPointerException) {
                            e.printStackTrace()
                        }
                    }

                    override fun processGsm(cell: CellGsm) {
                        try {
                            var num = (Objects.requireNonNull(
                                cell.band!!.number
                            ) as Int).toString()
                            val name = cell.band.name
                            if (num == "900") {
                                num = "8"
                            }
                            banda = "B$num - $name"
                            rssi = java.lang.Float.valueOf(cell.signal.rssi!!.toInt().toFloat())
                        } catch (e: NullPointerException) {
                            e.printStackTrace()
                        }
                    }

                    override fun processLte(cell: CellLte) {
                        try {
                            val num = (Objects.requireNonNull(
                                cell.band!!.number
                            ) as Int).toString()
                            val name = cell.band.name
                            banda = "B$num - $name"
                            rssi = java.lang.Float.valueOf(cell.signal.rssi!!.toInt().toFloat())
                        } catch (e: NullPointerException) {
                            e.printStackTrace()
                        }
                    }

                    override fun processNr(cell: CellNr) {
                        try {
                            val num = (Objects.requireNonNull(
                                cell.band!!.number
                            ) as Int).toString()
                            val name = cell.band.name
                            banda = "B$num - $name"
                        } catch (e: NullPointerException) {
                            e.printStackTrace()
                        }
                    }

                    override fun processTdscdma(cell: CellTdscdma) {
                        try {
                            val num = (Objects.requireNonNull(
                                cell.band!!.number
                            ) as Int).toString()
                            val name = cell.band.name
                            banda = "B$num - $name"
                            rssi =
                                java.lang.Float.valueOf(cell.signal.rssi!!.toInt().toFloat())
                        } catch (e: NullPointerException) {
                            e.printStackTrace()
                        }
                    }

                    // com.marlon.cz.mroczis.netmonster.core.model.cell.ICellProcessor
                    override fun processWcdma(cell: CellWcdma) {
                        try {
                            val num = (Objects.requireNonNull(
                                cell.band!!.number
                            ) as Int).toString()
                            val name = cell.band.name
                            banda = "B$num-$name"
                            rssi =
                                java.lang.Float.valueOf(cell.signal.rscp!!.toInt().toFloat())
                        } catch (e: NullPointerException) {
                            e.printStackTrace()
                        }
                    }
                }
                if (cells.isEmpty()) {
                    return null
                }
                cells[0].let(r0)
                return null
            }

            @Deprecated("Deprecated in Java")
            public override fun onPostExecute(r4: Void?) {
                tvSignal.text = "$banda MHz"
                bv.setValue(rssi)
                super.onPostExecute(r4)
            }
        }
    }

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

    companion object {
        private var context: Context? = null
        private var titleLayout: LinearLayout? = null

        // PROMO ETECSA CAROUSEL
        private var carouselLayout: CardView? = null
        private var sliderView: SliderView? = null
        var navigationView: NavigationView? = null
        private var punViewModel: PunViewModel? = null
        @JvmStatic
        fun insertNotification(pun: PUNotification?) {
            punViewModel!!.insertPUN(null)
        }

        const val PICK_CONTACT_REQUEST = 1
        fun showConnectedTime(status: Boolean) {
            FloatingBubbleService.showConnectedTime(status)
        }

        fun setConnectedTime(time: String?) {
            FloatingBubbleService.setConnectedTime(time)
        }

        @JvmStatic
        fun openLink(link: String?) {
            try {
                //JCLogging.message("Opening PROMO URL::url=" + link, null);
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

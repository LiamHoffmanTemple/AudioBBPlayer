package edu.temple.AudioBBPlayer

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import edu.temple.AudioBBPlayer.BookListFragment.BookSelectedInterface
import edu.temple.AudioBBPlayer.BookListFragment.Companion.newInstance
import android.util.Log
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.*
import edu.temple.audlibplayer.PlayerService
import android.widget.SeekBar



class MainActivity : AppCompatActivity(), BookSelectedInterface, PlayerFragment.PlayerFragmentInterface
{

    lateinit var binder: PlayerService.MediaControlBinder
    lateinit  var fm: FragmentManager
    var twoPane = false
    var bookDetailsFragment: BookDetailsFragment? = null
    var audioPlayerFragment: PlayerFragment? = null
    var selectedBook: Book? = null

    var bookListData: BookList? = null
    var currentProgress: Int =0
    var connect= false
    private val KEY_SELECTED_BOOK = "selectedBook"
    private val KEY_BOOK_LIST = "bookList"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bindService(Intent(this, PlayerService::class.java), serviceConnection, BIND_AUTO_CREATE)
        findViewById<Button>(R.id.buttonSecondActivity).setOnClickListener {
            openSomeActivityForResult()
        }


        if (savedInstanceState != null) {
            selectedBook = savedInstanceState.getParcelable(KEY_SELECTED_BOOK)
            bookListData = savedInstanceState.getParcelable(KEY_BOOK_LIST)
            twoPane = true

        }


        twoPane = findViewById<View?>(R.id.container2) != null

        fm = supportFragmentManager
        val fragment1: Fragment? = fm.findFragmentById(R.id.container1)
        fm.beginTransaction()
            .replace(R.id.container3, PlayerFragment())
            .commit()



        if (fragment1 is BookDetailsFragment) {
            fm.popBackStack()
        } else if (fragment1 !is BookListFragment) {
            fm.beginTransaction()
                .add(R.id.container1, newInstance(bookListData))
                .commit()
        }
        bookDetailsFragment =
            if (selectedBook == null)
                BookDetailsFragment()
            else
                BookDetailsFragment.newInstance(selectedBook)

        audioPlayerFragment =
            if (selectedBook == null)
                PlayerFragment()
            else
                PlayerFragment.newInstance(selectedBook)




        if (twoPane) {
            fm.beginTransaction()
                .replace(R.id.container2, bookDetailsFragment!!)
                .replace(R.id.container3,audioPlayerFragment!!)
                .commit()
        } else if (selectedBook != null) {
            fm.beginTransaction()
                .replace(R.id.container1, bookDetailsFragment!!)
                .replace(R.id.container3,audioPlayerFragment!!)
                .addToBackStack(null)
                .commit()
        }
    }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(KEY_SELECTED_BOOK, selectedBook)
        outState.putParcelable(KEY_BOOK_LIST, bookListData)
    }

    override fun onBackPressed() {

        selectedBook = null
        super.onBackPressed()
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(serviceConnection)
    }

    private var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val books = data?.getParcelableExtra<Parcelable>("SearchBooks") as BookList

                bookListData = books
                if (bookListData != null) {
                    fm = supportFragmentManager
                    fm.beginTransaction()
                        .replace(R.id.container1, newInstance(bookListData))
                        .commit()
                }
                if (twoPane) {
                    fm.beginTransaction()
                        .replace(R.id.container2, bookDetailsFragment!!)
                        .replace(R.id.container3, audioPlayerFragment!!)
                        .replace(R.id.container1, newInstance(bookListData))
                        .commit()
                }
            }
        }

    private val testBooks: BookList
        get() {
            val books = BookList()
            for (i in 1..10) {
                books.add(Book(i, "Book$i", "Author$i", "url$i", i));
            }
            return books
        }


    private fun openSomeActivityForResult() {
        val intent = Intent(this, BookSearchActivity::class.java)
        resultLauncher.launch(intent)
    }

    val progressHandler = Handler(Looper.getMainLooper()){

        if(it.obj != null) {
            val bookProgress = it.obj as PlayerService.BookProgress

            currentProgress = bookProgress.progress
            Log.i(
                "PROGRESS HANDLER",
                "progress time ?  $currentProgress",
            )
            var seekBar = findViewById<SeekBar>(R.id.seekBar)
            seekBar.progress = currentProgress

        }
        true
    }

    val serviceConnection = object: ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            connect = true
            binder = service as PlayerService.MediaControlBinder
            binder.setProgressHandler(progressHandler)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            connect = false
        }

    }



    override fun bookSelected(book: Book) {
        selectedBook = book
        if (twoPane){
            bookDetailsFragment!!.displayBook(selectedBook!!)
            audioPlayerFragment!!.book = selectedBook

        }
        else {
            fm.beginTransaction()
                .replace(R.id.container1, BookDetailsFragment.newInstance(selectedBook))
                .replace(R.id.container3,PlayerFragment.newInstance(selectedBook))
                .addToBackStack(null)
                .commit()
        }
    }
    override fun play() {
        var book = selectedBook
        if(book != null&&connect)
            binder.play(book.id)
    }


    override fun pause() {
        if(connect && binder.isBinderAlive) {
            binder.pause()
        }
    }

    override fun stop() {
        if(connect && binder.isBinderAlive) {
            binder.stop()
        }
    }

    override fun seekbarChange() {
        if(connect && binder.isBinderAlive) {
            binder.seekTo(currentProgress)

        }
    }
}

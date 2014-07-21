package epu.no1.app;

import java.util.ArrayList;
import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnKeyListener;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import epu.no1.app.helper.JSONParser;

public class MessageActivity extends ListActivity implements
		TextToSpeech.OnInitListener, OnClickListener {
	/** Called when the activity is first created. */
	private static final int VOICE_RECOGNITION_REQUEST_CODE = 1234;
	private ArrayList<Message> messages;
	private AwesomeAdapter adapter;
	private EditText text;

	Locale loc = new Locale("VIETNAM", "Vietnamese");

	private TextToSpeech tts;
	private MediaPlayer mediaPlayer;
	private String result = "";
	private static String url;
	protected static final int RESULT_SPEECH = 1;
	public static String answer;
	Button btnSend, btnSpeech;

	ProgressDialog dialog;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		tts = new TextToSpeech(this, this);

		mediaPlayer = new MediaPlayer();
		mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

		text = (EditText) this.findViewById(R.id.text);
		btnSend = (Button) findViewById(R.id.btnSend);
		btnSend.setOnClickListener(this);
		btnSpeech = (Button) findViewById(R.id.btnSpeech);
		btnSpeech.setOnClickListener(this);

		messages = new ArrayList<Message>();

		adapter = new AwesomeAdapter(this, messages);
		setListAdapter(adapter);
		//
		dialog = new ProgressDialog(this);
		dialog.setMessage("Đang xử lý...");
		this.dialog.setCanceledOnTouchOutside(false);
		dialog.setOnKeyListener(new OnKeyListener() {

			@Override
			public boolean onKey(DialogInterface dialog, int keyCode,
					KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_BACK) {
					dialog.dismiss();
				}
				return false;
			}
		});

	}

	private class SendMessage extends AsyncTask<Void, String, JSONObject> {
		@Override
		protected JSONObject doInBackground(Void... params) {

			this.publishProgress(String.format("Kiểm tra dữ liệu..."));

			JSONObject result = null;

			JSONParser jParser = new JSONParser();

			result = jParser.getJSONFromUrl(url);
			return result;

		}

		@Override
		public void onProgressUpdate(String... v) {

			if (messages.get(messages.size() - 1).isStatusMessage) {
				messages.get(messages.size() - 1).setMessage(v[0]);
				adapter.notifyDataSetChanged();
				getListView().setSelection(messages.size() - 1);
			} else {
				addNewMessage(new Message(true, v[0])); // add new message
			}
		}

		@Override
		protected void onPostExecute(JSONObject result) {
			if (messages.get(messages.size() - 1).isStatusMessage)// neu la noi
																	// dung
																	// trang
																	// thai thi
																	// xoa di
			{
				messages.remove(messages.size() - 1);
			}
			answer = null;
			try {
				if (result.getString("status").equals("success")) {
					answer = result.getString("response");
				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			addNewMessage(new Message(answer, false)); // add cau tra loi duoc
														// lay tu server
			speakOut();

		}

	}

	void addNewMessage(Message m) {
		messages.add(m);
		adapter.notifyDataSetChanged();
		getListView().setSelection(messages.size() - 1);
	}

	@Override
	public void onDestroy() {
		// Don't forget to shutdown!
		if (tts != null) {
			tts.stop();
			tts.shutdown();
		}
		super.onDestroy();
	}

	@Override
	public void onInit(int status) {
		// TODO Auto-generated method stub

		if (status == TextToSpeech.SUCCESS) {

			int result = tts.setLanguage(loc);

			// tts.setPitch(5); // set pitch level

			// tts.setSpeechRate(2); // set speech speed rate

			if (result == TextToSpeech.LANG_MISSING_DATA
					|| result == TextToSpeech.LANG_NOT_SUPPORTED) {
				Log.e("TTS", "Language is not supported");
			} else {
				speakOut();
			}

		} else {
			Log.e("TTS", "Initilization Failed");
		}

	}

	private void speakOut() {

		String text = answer;

		tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
	}

	public String chuanhoa(String s) {
		String result = null;
		result = s.replaceAll(" ", "%20");
		return result;
	}

	public boolean isConnectingToInternet() {
		ConnectivityManager connectivity = (ConnectivityManager) MessageActivity.this
				.getApplicationContext().getSystemService(
						Context.CONNECTIVITY_SERVICE);
		if (connectivity != null) {
			NetworkInfo[] info = connectivity.getAllNetworkInfo();
			if (info != null)
				for (int i = 0; i < info.length; i++)
					if (info[i].getState() == NetworkInfo.State.CONNECTED) {
						return true;
					}
		}
		return false;
	}

	private void sendMess() {
		String newMessage = text.getText().toString().trim();
		if (newMessage.length() > 0) {
			text.setText("");
			url = "http://tech.fpt.com.vn/AIML/api/bots/53b62650e4b02d633752cd04/chat?request="
					+ chuanhoa(newMessage)
					+ "&token=29a2804f-631d-4319-8c04-d0baeb5f6ca9";
			addNewMessage(new Message(newMessage, true));
			if (isConnectingToInternet()) {
				new SendMessage().execute();
			}
		}
	}

	private void startVoiceRecognitionActivity() {
		Intent localIntent = new Intent(
				RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		localIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN");
		localIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,
				"vi-VN");
		localIntent
				.putExtra(
						RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE,
						"vi-VN");
		localIntent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speaking...");
		try {
			startActivityForResult(localIntent, VOICE_RECOGNITION_REQUEST_CODE);
		} catch (Exception ex) {
			Toast t = Toast.makeText(getApplicationContext(),
					"Ops! Your device doesn't support Speech to Text",
					Toast.LENGTH_SHORT);
			t.show();
			Intent localIntentx = new Intent();
			localIntent.setAction("android.speech.tts.engine.INSTALL_TTS_DATA");
			startActivity(localIntentx);
		}
	}

	@Override
	protected void onActivityResult(int paramInt1, int paramInt2,
			Intent paramIntent) {
		if ((paramInt1 == VOICE_RECOGNITION_REQUEST_CODE) && (paramInt2 == -1)) {
			result = ((String) paramIntent.getStringArrayListExtra(
					"android.speech.extra.RESULTS").get(0));
			text.setText(result);
			sendMess();
			super.onActivityResult(paramInt1, paramInt2, paramIntent);
		}
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch (v.getId()) {
		case R.id.btnSend:
			sendMess();
			break;
		case R.id.btnSpeech:
			startVoiceRecognitionActivity();
			break;
		default:
			break;
		}
	}

}

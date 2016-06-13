package com.igeak.example.datalayer;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.igeak.android.common.ConnectionResult;
import com.igeak.android.common.api.GeakApiClient;
import com.igeak.android.common.api.GeakApiClient.ConnectionCallbacks;
import com.igeak.android.common.api.GeakApiClient.OnConnectionFailedListener;
import com.igeak.android.common.data.FreezableUtils;
import com.igeak.android.wearable.Asset;
import com.igeak.android.wearable.DataApi;
import com.igeak.android.wearable.DataEvent;
import com.igeak.android.wearable.DataEventBuffer;
import com.igeak.android.wearable.DataMapItem;
import com.igeak.android.wearable.MessageApi;
import com.igeak.android.wearable.MessageEvent;
import com.igeak.android.wearable.Node;
import com.igeak.android.wearable.NodeApi;
import com.igeak.android.wearable.Wearable;

import java.io.InputStream;
import java.util.List;

import static com.igeak.example.datalayer.DataLayerListenerService.LOGD;

/**
 * Shows events and photo from the Wearable APIs.
 */
public class MainActivity extends Activity implements ConnectionCallbacks,
        OnConnectionFailedListener, DataApi.DataListener, MessageApi.MessageListener,
        NodeApi.NodeListener {

    private static final String TAG = "MainActivity";

    private GeakApiClient mMobvoiApiClient;
    private ListView mDataItemList;
    private TextView mIntroText;
    private DataItemAdapter mDataItemListAdapter;
    private View mLayout;
    private Handler mHandler;

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        mHandler = new Handler();
        LOGD(TAG, "onCreate");
        setContentView(R.layout.main_activity);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mDataItemList = (ListView) findViewById(R.id.dataItem_list);
        mIntroText = (TextView) findViewById(R.id.intro);
        mLayout = findViewById(R.id.layout);

        // Stores data events received by the local broadcaster.
        mDataItemListAdapter = new DataItemAdapter(this, android.R.layout.simple_list_item_1);
        mDataItemList.setAdapter(mDataItemListAdapter);

        mMobvoiApiClient = new GeakApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMobvoiApiClient.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Wearable.DataApi.removeListener(mMobvoiApiClient, this);
        Wearable.MessageApi.removeListener(mMobvoiApiClient, this);
        Wearable.NodeApi.removeListener(mMobvoiApiClient, this);
        mMobvoiApiClient.disconnect();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Wearable.DataApi.addListener(mMobvoiApiClient, this);
        Wearable.MessageApi.addListener(mMobvoiApiClient, this);
        Wearable.NodeApi.addListener(mMobvoiApiClient, this);
    }

    @Override
    public void onConnectionSuspended(int cause) {
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.e(TAG, "onConnectionFailed(): Failed to connect, with result: " + result);
    }

    private void generateEvent(final String title, final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mIntroText.setVisibility(View.INVISIBLE);
                mDataItemListAdapter.add(new Event(title, text));
            }
        });
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        LOGD(TAG, "onDataChanged(): " + dataEvents);

        final List<DataEvent> events = FreezableUtils.freezeIterable(dataEvents);
        dataEvents.close();
        for (DataEvent event : events) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                String path = event.getDataItem().getUri().getPath();
                if (DataLayerListenerService.IMAGE_PATH.equals(path)) {
                    DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                    Asset photo = dataMapItem.getDataMap()
                            .getAsset(DataLayerListenerService.IMAGE_KEY);
                    final Bitmap bitmap = loadBitmapFromAsset(mMobvoiApiClient, photo);
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Log.d(TAG, "Setting background image..");
                            mLayout.setBackground(new BitmapDrawable(getResources(), bitmap));
                        }
                    });

                } else if (DataLayerListenerService.COUNT_PATH.equals(path)) {
                    LOGD(TAG, "Data Changed for COUNT_PATH");
                    generateEvent("DataItem Changed", event.getDataItem().toString());
                } else {
                    LOGD(TAG, "Unrecognized path: " + path);
                }

            } else if (event.getType() == DataEvent.TYPE_DELETED) {
                generateEvent("DataItem Deleted", event.getDataItem().toString());
            } else {
                generateEvent("Unknown data event type", "Type = " + event.getType());
            }
        }
    }

    /**
     * Extracts {@link Bitmap} data from the
     * {@link com.igeak.android.wearable.Asset}
     */
    private Bitmap loadBitmapFromAsset(GeakApiClient apiClient, Asset asset) {
        if (asset == null) {
            throw new IllegalArgumentException("Asset must be non-null");
        }

        InputStream assetInputStream = Wearable.DataApi.getFdForAsset(
                apiClient, asset).await().getInputStream();

        if (assetInputStream == null) {
            Log.w(TAG, "Requested an unknown Asset.");
            return null;
        }
        return BitmapFactory.decodeStream(assetInputStream);
    }

    @Override
    public void onMessageReceived(MessageEvent event) {
        LOGD(TAG, "onMessageReceived: " + event);
        generateEvent("Message", event.toString());
    }

    @Override
    public void onPeerConnected(Node node) {
        generateEvent("Node Connected", node.getId());
    }

    @Override
    public void onPeerDisconnected(Node node) {
        generateEvent("Node Disconnected", node.getId());
    }

    private static class DataItemAdapter extends ArrayAdapter<Event> {

        private final Context mContext;

        public DataItemAdapter(Context context, int unusedResource) {
            super(context, unusedResource);
            mContext = context;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                holder = new ViewHolder();
                LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(android.R.layout.two_line_list_item, null);
                convertView.setTag(holder);
                holder.text1 = (TextView) convertView.findViewById(android.R.id.text1);
                holder.text2 = (TextView) convertView.findViewById(android.R.id.text2);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            Event event = getItem(position);
            holder.text1.setText(event.title);
            holder.text2.setText(event.text);
            return convertView;
        }

        private class ViewHolder {

            TextView text1;
            TextView text2;
        }
    }

    private class Event {

        String title;
        String text;

        public Event(String title, String text) {
            this.title = title;
            this.text = text;
        }
    }
}

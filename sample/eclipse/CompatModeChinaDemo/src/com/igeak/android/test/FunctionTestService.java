//Copyright 2015 Mobvoi Inc. All Rights Reserved
package com.igeak.android.test;

import com.igeak.android.common.api.GeakApiClient;
import com.igeak.android.common.api.ResultCallback;
import com.igeak.android.wearable.DataApi;
import com.igeak.android.wearable.DataApi.GetFdForAssetResult;
import com.igeak.android.wearable.DataEvent;
import com.igeak.android.wearable.DataEventBuffer;
import com.igeak.android.wearable.DataItem;
import com.igeak.android.wearable.DataItemAsset;
import com.igeak.android.wearable.MessageEvent;
import com.igeak.android.wearable.Wearable;
import com.igeak.android.wearable.WearableListenerService;

import java.io.InputStream;

public class FunctionTestService extends WearableListenerService {
    
    private GeakApiClient client;
    
    @Override
    public void onCreate() {
        super.onCreate();
        client = new GeakApiClient.Builder(this).addApi(Wearable.API).build();
        client.connect();
    }
    
    @Override
    public void onDataChanged(DataEventBuffer buffer) {
        if (buffer != null && buffer.getCount() > 0) {
            DataEvent e = buffer.get(0);
            if (e.getDataItem() != null) {
                DataItem item = e.getDataItem();
                if (item.getData() != null && item.getData().length > 0) {
                    Utils.setText(this, "receive",
                            "" + Utils.getHashCode(item.getData()));
                } else if (item.getAssets().containsKey("key")) {
                    DataItemAsset a = item.getAssets().get("key");
                    Wearable.DataApi.getFdForAsset(client, a).setResultCallback(
                            new ResultCallback<DataApi.GetFdForAssetResult>() {
                        @Override
                        public void onResult(GetFdForAssetResult result) {
                            if (result.getStatus().isSuccess()) {
                                InputStream in = result.getInputStream();
                                Utils.setText(FunctionTestService.this, "receive",
                                        "" + Utils.readAll(in));
                            }
                        }
                    });
                }
            }
        }
    }

    @Override
    public void onMessageReceived(MessageEvent messageevent) {
        if (messageevent != null && messageevent.getData() != null) {
            Utils.setText(this, "receive", "" + Utils.getHashCode(messageevent.getData()));
        }
    }
    
}

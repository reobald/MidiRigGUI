/* Copyright (C) 2015-2017 Patrik Jonasson - All Rights Reserved
*
*
* This file is part of MidiRig.
*
* MidiRig is free software: you can redistribute it and/or modify it 
* under the terms of the GNU General Public License as published by 
* the Free Software Foundation, either version 3 of the License, 
* or (at your option) any later version.
*
* MidiRig is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
* without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
* See the GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License along with MidiRig.  
* If not, see <http://www.gnu.org licenses/>.
*/

package com.electrophone.midirig;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import java.net.UnknownHostException;

public class MainActivity extends Activity implements LogConstant {

    public static final String SAVED_SCENES = "SAVED_SCENES";
    public static final String SAVED_CURRENT_SCENE = "SAVED_CURRENT_SCENE";
    OSCUpdatesHandler handler;
    private SceneInfoMap scenes = null;
    private OSCReceiver oscReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initPreferences();

        handler = new OSCUpdatesHandler(this);

        oscReceiver = new OSCReceiver(this);
        oscReceiver.startOSCserver();
        try {
//            wait for server to become active
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (savedInstanceState != null) {

            scenes = savedInstanceState.getParcelable(SAVED_SCENES);

            if ((scenes == null) || (scenes.isEmpty())) {
                log("transmit query");
                transmitQuery();
            } else {
                log("Recreating saved instance state");

                int currentScene = savedInstanceState.getInt(SAVED_CURRENT_SCENE);
                if (currentScene > -1)
                    updateCurrentScene(currentScene);
                updateScenelistFragment();
            }

        } else {
            transmitQuery();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.settings:
                Intent i = new Intent(com.electrophone.midirig.MainActivity.this, com.electrophone.midirig.SettingsActivity.class);
                startActivity(i);
            case R.id.refresh:
                transmitQuery();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        log("Saving instance state");
        outState.putParcelable(SAVED_SCENES, scenes);
        //SceneInfo currentScene = getCurrentScene();
        outState.putInt(SAVED_CURRENT_SCENE, getCurrentScene());
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestart() {
        log("Restarting");
        super.onRestart();
        oscReceiver.restartOSCserver();
    }

    @Override
    protected void onStop() {
        log("Stopping");
        super.onStop();
        oscReceiver.stop();
    }

    @Override
    protected void onDestroy() {
        log("destroying");
        super.onDestroy();
        oscReceiver.destroy();
    }

    private void transmitQuery() {
        OSCTransmitter transmitter = new OSCTransmitter(this);
        try {
            MidiDingsOSCParams params = new MidiDingsOSCParams(this, MidiDingsOSCParams.QUERY);
            log("OSCparams: " + params.toString());
            transmitter.execute(params);
        } catch (UnknownHostException e) {
            log(e.getMessage());
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public int getCurrentScene() {
        log("Get current scene");
        SceneItemFragment sceneItemFragment = (SceneItemFragment) getFragmentManager().findFragmentById(R.id.sceneItemFragment);
        return (sceneItemFragment.getSceneNumber());
    }

    public void updateCurrentScene(int sceneNumber) {
        log("Update current scene");
        SceneInfo sceneInfo = scenes.get(sceneNumber);
        SceneItemFragment sceneItemFragment = (SceneItemFragment) getFragmentManager().findFragmentById(R.id.sceneItemFragment);
        sceneItemFragment.setSceneInfo(sceneInfo);
    }

    public void updateScenelistFragment() {
        log("Update current scene list");
        if (scenes != null) {
            SceneListFragment sceneListFragment = (SceneListFragment) getFragmentManager().findFragmentById(R.id.sceneListFragment);
            sceneListFragment.setSceneList(scenes.values());
        }
    }

    public void updateScenes(SceneInfoMap scenes) {
        this.scenes = scenes;
        updateScenelistFragment();
    }

    public Handler getHandler() {
        return handler;
    }


    private void log(String msg) {
        Log.d(LOG_TAG, msg);
    }

    private void initPreferences() {
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
    }

}


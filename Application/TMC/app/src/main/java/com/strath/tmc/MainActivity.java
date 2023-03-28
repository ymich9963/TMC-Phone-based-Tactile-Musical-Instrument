package com.strath.tmc; //unique package name

import android.content.pm.PackageManager;
import androidx.appcompat.app.AppCompatActivity;

//imports for the Graphical User Interface
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.ToggleButton;

//imports for MIDI functionality
import android.media.midi.MidiDevice.MidiConnection;
import android.media.midi.MidiDeviceInfo;
import android.media.midi.MidiManager;

//imports from the provided Android Example
import com.example.android.common.midi.MidiOutputPortConnectionSelector;
import com.example.android.common.midi.MidiPortConnector;
import com.example.android.common.midi.MidiTools;

public class MainActivity extends AppCompatActivity {
    //variable initialisations
    boolean set = false;
    boolean effects_selected = false, sound_selected = false, spinner_selected = false;

    private MidiOutputPortConnectionSelector mPortSelector;




    @Override
    protected void onCreate(Bundle savedInstanceState) { //function is called when the app is opened
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SelectionConfig.init(false, 0, 0, 0); //initialise a SelectionConfig class

        //create variables for the spinner and the toggle button
        Spinner midi_spinner = (Spinner) findViewById(R.id.spinner_synth_sender);
        ToggleButton toggle = (ToggleButton) findViewById(R.id.tbSetReset); //initialise a toggle button
        //disable them
        toggle.setEnabled(false);
        midi_spinner.setEnabled(false);

        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_MIDI)) { //if statement to check if MIDI is supported
            setupMidi();
            //disable spinner
            midi_spinner.setEnabled(false);
            midi_spinner.setSelection(0);
            //disable toggle
            toggle.setAlpha(0.5f);
            toggle.setEnabled(false);
        } else{
            Toast.makeText(MainActivity.this, "MIDI not supported!", Toast.LENGTH_LONG).show(); //output an error if not supported

        }
    }

    private void setupMidi() {
        //variable initialisations required to setup the MIDI connection
        MidiManager midiManager;
        midiManager = (MidiManager) getSystemService(MIDI_SERVICE);
        MidiDeviceInfo synthInfo = MidiTools.findDevice(midiManager, "AndroidTest", "SynthExample");
        int portIndex = 0;
        mPortSelector = new MidiOutputPortConnectionSelector(midiManager, this, R.id.spinner_synth_sender, synthInfo, portIndex);
        mPortSelector.setConnectedListener(new MyPortsConnectedListener());
    }

    private void closeSynthResources() { //to close the port
        if (mPortSelector != null) {
            mPortSelector.close();
        }
    }

    private class MyPortsConnectedListener implements MidiPortConnector.OnPortsConnectedListener {
        @Override
        public void onPortsConnected(final MidiConnection connection) {
            runOnUiThread(() -> {
                if (connection == null) {
                    Toast.makeText(MainActivity.this, "Error: Device Not Connected", Toast.LENGTH_LONG).show();
                    mPortSelector.clearSelection();
                } else {
                    Toast.makeText(MainActivity.this, "Device Connected", Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    @Override
    public void onDestroy() { //to stop everything
        closeSynthResources();
        super.onDestroy();
    }



    public void onCheckboxClicked(View view) { //when the checkbox is clicked this function executes

        //create variables for each checkbox and the toggle button
        CheckBox cb_sin = findViewById(R.id.cb_sin);
        CheckBox cb_saw = findViewById(R.id.cb_saw);

        CheckBox cb_pitch = findViewById(R.id.cb_pitch);
        CheckBox cb_duration = findViewById(R.id.cb_duration);
        CheckBox cb_bend_up = findViewById(R.id.cb_bend_up);
        CheckBox cb_bend_down = findViewById(R.id.cb_bend_down);

        ToggleButton toggle = (ToggleButton) findViewById(R.id.tbSetReset); //initialise a toggle button

        //code for setting sound id in config class and disabling the correct checkbox
        if(cb_sin.isChecked()){ //if it is checked, disable the other box
            cb_saw.setEnabled(false);
            SelectionConfig.setSound_id(1);
            sound_selected = true;
        } else if(cb_saw.isChecked()){
            cb_sin.setEnabled(false);
            SelectionConfig.setSound_id(2);
            sound_selected = true;
        } else{
            cb_sin.setEnabled(true);
            cb_saw.setEnabled(true);
            sound_selected = false;
        }

        //to set effect checkbox id in config class
        //this block is for the pitch effect with effect ID of 1
        if(cb_pitch.isChecked()){ //if the checkbox is checked
            if(SelectionConfig.getEffect_id1() == 0){ //only set values if they are 0
                SelectionConfig.setEffect_id1(1); //set the effect id
                //only set the value of the second effect is the current value of effect id 2 is 0 and effect id 1 is not the selected checkbox
            } else if(SelectionConfig.getEffect_id2() == 0 && SelectionConfig.getEffect_id1() != 1 ){
                SelectionConfig.setEffect_id2(1);
            }
        } else if (SelectionConfig.getEffect_id1() == 1){ //checks if when checkbox is unchecked, the effect value set is the one selected
            SelectionConfig.setEffect_id1(0); //if it is then reset the value back to 0
        } else if (SelectionConfig.getEffect_id2() == 1){
            SelectionConfig.setEffect_id2(0);
        }

        //this block is for the duration effect with effect ID of 2
        if(cb_duration.isChecked()) { //same as previous happens for each checkbox
            if (SelectionConfig.getEffect_id1() == 0) {
                SelectionConfig.setEffect_id1(2);
            } else if(SelectionConfig.getEffect_id2() == 0 && SelectionConfig.getEffect_id1() != 2 ){
                SelectionConfig.setEffect_id2(2);
            }
        } else if (SelectionConfig.getEffect_id1() == 2){
            SelectionConfig.setEffect_id1(0);
        } else if (SelectionConfig.getEffect_id2() == 2){
            SelectionConfig.setEffect_id2(0);
        }

        //this block is for the pitch bend up effect with effect ID of 3
        if (cb_bend_up.isChecked()) {
            if (SelectionConfig.getEffect_id1() == 0) {
                SelectionConfig.setEffect_id1(3);
            } else if(SelectionConfig.getEffect_id2() == 0 && SelectionConfig.getEffect_id1() != 3 ){
                SelectionConfig.setEffect_id2(3);
            }
        } else if (SelectionConfig.getEffect_id1() == 3){
            SelectionConfig.setEffect_id1(0);
        } else if (SelectionConfig.getEffect_id2() == 3){
            SelectionConfig.setEffect_id2(0);
        }

        //this block is for the pitch bend down effect with effect ID of 4
        if (cb_bend_down.isChecked()) {
            if (SelectionConfig.getEffect_id1() == 0) {
                SelectionConfig.setEffect_id1(4);
            } else if(SelectionConfig.getEffect_id2() == 0 && SelectionConfig.getEffect_id1() != 4 ){
                SelectionConfig.setEffect_id2(4);
            }
        } else if (SelectionConfig.getEffect_id1() == 4){
            SelectionConfig.setEffect_id1(0);
        } else if (SelectionConfig.getEffect_id2() == 4){
            SelectionConfig.setEffect_id2(0);
        }

        //code to disable the correct checkboxes after selection
        if(cb_pitch.isChecked() && cb_duration.isChecked()){
            cb_bend_up.setEnabled(false);
            cb_bend_down.setEnabled(false);
            effects_selected = true;
        } else if(cb_pitch.isChecked() && cb_bend_up.isChecked()){
            cb_duration.setEnabled(false);
            cb_bend_down.setEnabled(false);
            effects_selected = true;
        } else if(cb_pitch.isChecked() && cb_bend_down.isChecked()){
            cb_duration.setEnabled(false);
            cb_bend_up.setEnabled(false);
            effects_selected = true;
        } else if(cb_duration.isChecked() && cb_bend_up.isChecked()){
            cb_pitch.setEnabled(false);
            cb_bend_down.setEnabled(false);
            effects_selected = true;
        }else if(cb_duration.isChecked() && cb_bend_down.isChecked()){
            cb_pitch.setEnabled(false);
            cb_bend_up.setEnabled(false);
            effects_selected = true;
        }else if(cb_bend_up.isChecked() && cb_bend_down.isChecked()){
            cb_pitch.setEnabled(false);
            cb_duration.setEnabled(false);
            effects_selected = true;
        } else{
            cb_pitch.setEnabled(true);
            cb_duration.setEnabled(true);
            cb_bend_up.setEnabled(true);
            cb_bend_down.setEnabled(true);
            effects_selected = false;
        }

        //check if effects and sounds are selected
        if((effects_selected) && (sound_selected)){ //if true, enable the spinner
            toggle.setAlpha(1f);
            toggle.setEnabled(true);
        } else { //if false, disable it and reset selection
            toggle.setAlpha(0.5f);
            toggle.setEnabled(false);
        }

        //for debugging
        //Toast.makeText(MainActivity.this, String.valueOf(SelectionConfig.getEffect_id1()), Toast.LENGTH_SHORT).show();
        //Toast.makeText(MainActivity.this, String.valueOf(SelectionConfig.getEffect_id2()), Toast.LENGTH_SHORT).show();
    }


    public void onButtonClicked(View view) {
        //create variables for each checkbox, the toggle button, and the spinner
        CheckBox cb_sin = findViewById(R.id.cb_sin);
        CheckBox cb_saw = findViewById(R.id.cb_saw);

        CheckBox cb_pitch = findViewById(R.id.cb_pitch);
        CheckBox cb_duration = findViewById(R.id.cb_duration);
        CheckBox cb_bend_up = findViewById(R.id.cb_bend_up);
        CheckBox cb_bend_down = findViewById(R.id.cb_bend_down);

        ToggleButton toggle = (ToggleButton) findViewById(R.id.tbSetReset); //initialise a toggle button

        Spinner midi_spinner = (Spinner) findViewById(R.id.spinner_synth_sender);

        if(SelectionConfig.isSet()){ //if the button is pressed when "Reset" is shown
            //uncheck all check boxes
            cb_sin.setChecked(false);
            cb_saw.setChecked(false);
            cb_pitch.setChecked(false);
            cb_duration.setChecked(false);
            cb_bend_up.setChecked(false);
            cb_bend_down.setChecked(false);

            //enable all checkboxes
            cb_sin.setEnabled(true);
            cb_saw.setEnabled(true);
            cb_pitch.setEnabled(true);
            cb_duration.setEnabled(true);
            cb_bend_up.setEnabled(true);
            cb_bend_down.setEnabled(true);

            //disable the toggle button just like in startup
            toggle.setAlpha(0.5f);
            toggle.setEnabled(false);

            //reset selector variables
            effects_selected = false;
            sound_selected = false;
            spinner_selected = false;

            //disable spinner
            midi_spinner.setEnabled(false);
            midi_spinner.setSelection(0);

            //reset all variables
            SelectionConfig.setSound_id(0);
            SelectionConfig.setEffect_id1(0);
            SelectionConfig.setEffect_id2(0);

            } else{ //if it is pressed when "Set" is shown
            //disable all checkboxes
            cb_sin.setEnabled(false);
            cb_saw.setEnabled(false);
            cb_pitch.setEnabled(false);
            cb_duration.setEnabled(false);
            cb_bend_up.setEnabled(false);
            cb_bend_down.setEnabled(false);

            //enable spinner
            midi_spinner.setEnabled(true);

            //error checking in case the values get mixed up and the same effect is selected twice
            if(SelectionConfig.getEffect_id1() == SelectionConfig.getEffect_id2() && effects_selected) {
                //reset both effect ids to 0
                SelectionConfig.setEffect_id1(0);
                SelectionConfig.setEffect_id2(0);

                //uncheck all effects check boxes
                cb_pitch.setChecked(false);
                cb_duration.setChecked(false);
                cb_bend_up.setChecked(false);
                cb_bend_down.setChecked(false);

                //output an error as a Toast
                Toast.makeText(MainActivity.this, "ERROR: The same effects were detected. Please retry.", Toast.LENGTH_SHORT).show();
                effects_selected = false;

                //disable spinner
                midi_spinner.setEnabled(false);
                midi_spinner.setSelection(0);
            }

        }

        //pass the value of set to the class
        SelectionConfig.setSet(toggle.isChecked());

        //for debugging purposes
        //Toast.makeText(MainActivity.this, String.valueOf(SelectionConfig.getEffect_id1()), Toast.LENGTH_SHORT).show();
        //Toast.makeText(MainActivity.this, String.valueOf(SelectionConfig.getEffect_id2()), Toast.LENGTH_SHORT).show();

    }
}
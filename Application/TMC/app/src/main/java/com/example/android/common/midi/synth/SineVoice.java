/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.common.midi.synth;

/**
 * Replace sawtooth with a sine wave.
 */
public class SineVoice extends SawVoice {
    @Override
    protected SineOscillator createOscillator() {
        return new SineOscillator();
    }

    //---------ADDITIONS---------------------------------------------------------------------
    //created a Sine voice the same way the Saw voice was created but with the provided
    //Sine Oscillator class
    private SineOscillator mOscillator;
    private EnvelopeADSR mEnvelope;

    public SineVoice() {
        mOscillator = createOscillator();
        mEnvelope = new EnvelopeADSR();
    }

    @Override
    public void noteOn(int noteIndex, int velocity) {
        super.noteOn(noteIndex, velocity);
        mOscillator.setPitch(noteIndex);
        mOscillator.setAmplitude(getAmplitude());
        mEnvelope.on();
    }

    @Override
    public void noteOff() {
        super.noteOff();
        mEnvelope.off();
    }

    @Override
    public void setFrequencyScaler(float scaler) {
        mOscillator.setFrequencyScaler(scaler);
    }

    @Override
    public float render() {
        float output = mOscillator.render() * mEnvelope.render();
        return output;
    }

    @Override
    public boolean isDone() {
        return mEnvelope.isDone();
    }

//----------------------------------------------------------------------------------------------
}

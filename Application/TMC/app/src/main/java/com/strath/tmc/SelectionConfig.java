package com.strath.tmc;

//class created to store the user configurations
public class SelectionConfig{

    private static boolean set;
    private static int sound_id;
    private static int effect_id1;
    private static int effect_id2;

    //class method to initialise the static class
    public static void init(boolean set_value, int sound_id_value, int effect_id1_value, int effect_id2_value) {
        set = set_value;
        sound_id = sound_id_value;
        effect_id1 = effect_id1_value;
        effect_id2 = effect_id2_value;
    }

    //getters and setters for each variable
    public static boolean isSet() {
        return set;
    }

    public static void setSet(boolean set_value) {
        set = set_value;
    }

    public static int getSound_id() {
        return sound_id;
    }

    public static void setSound_id(int sound_id_value) {
        sound_id = sound_id_value;
    }

    public static int getEffect_id1() {
        return effect_id1;
    }

    public static void setEffect_id1(int effect_id1_value) {
        effect_id1 = effect_id1_value;
    }

    public static int getEffect_id2() {
        return effect_id2;
    }

    public static void setEffect_id2(int effect_id2_value) { effect_id2 = effect_id2_value; }

}

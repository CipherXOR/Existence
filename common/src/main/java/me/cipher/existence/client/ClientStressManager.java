package me.cipher.existence.client;

public class ClientStressManager {
    private static int targetLoad = 0;

    public static void setLoad(int load) {
        targetLoad = load;
    }

    public static int getLoad() {
        return targetLoad;
    }
}
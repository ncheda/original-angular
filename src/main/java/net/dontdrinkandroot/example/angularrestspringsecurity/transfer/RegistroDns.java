package net.dontdrinkandroot.example.angularrestspringsecurity.transfer;

import java.util.ArrayList;

/**
 * Created by kripton on 13/5/2016.
 */
public class RegistroDns {
    public RegistroDns(String dns, String ip, ArrayList<String> campos){
        this.dns = dns;
        this.ip = ip;
        this.campos = campos;
    }
    private final String ip;
    private final String dns;
    private final ArrayList<String> campos;

    public String getIp() {
        return ip;
    }

    public String getDns() {
        return dns;
    }

    public ArrayList<String> getCampos() {
        return campos;
    }
}

package net.dontdrinkandroot.example.angularrestspringsecurity.transfer;

import java.util.ArrayList;

/**
 * Created by kripton on 13/5/2016.
 */
public class ResultTransfer {

    public ResultTransfer(ArrayList<RegistroDns> resultado){
        this.resultado = resultado;
    }
    private  final ArrayList<RegistroDns> resultado;

    public ArrayList<RegistroDns> getResultado() {
        return resultado;
    }



}

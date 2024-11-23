package com.atoudeft.banque;

public class OperationFacture extends Operation{
    String numFacture;
    String descFacture;

    public OperationFacture(double montant, TypeOperation type,String numFacture,String descFacture){
        super(montant, type);
        this.numFacture = numFacture;
        this.descFacture = descFacture;
    }

    @Override
    public String toString() {
        return "DATE" + super.getDate() +
                "\nTYPE " + type +
                "\nMONTANT " + montant+
                "\nnumFacture "+ numFacture+
                "\ndescFacture "+ descFacture;
    }
}

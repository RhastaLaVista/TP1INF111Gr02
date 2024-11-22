package com.atoudeft.banque;

public class OperationTransfer extends Operation{
    String numdestinataire = "";
    public OperationTransfer(double montant,TypeOperation type, String numerodestinataire){
        super(montant,type);
        this.numdestinataire = numerodestinataire;
    }
    @Override
    public String toString() {
        return " DATE " + super.getDate() +
                " TYPE " + type +
                " MONTANT " + montant+
                " NUM DESTINATAIRE " + numdestinataire;
    }
}

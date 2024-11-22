package com.atoudeft.banque;

public class OperationDepot extends Operation{
    public OperationDepot(double montant, com.atoudeft.banque.TypeOperation type){
        super(montant, type);
    }

    @Override
    public String toString() {
        return  "DATE" + super.getDate() +
                "TYPE " + type +
                "MONTANT " + montant;
    }
}

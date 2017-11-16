package com.alflabs.rig4;

public class EntryPoint {
    public static void main(String[] args) {

        IRigComponent component = DaggerIRigComponent
                .builder()
                .rigModule(new RigModule())
                .build();

        System.out.println("Hello World!");
    }
}

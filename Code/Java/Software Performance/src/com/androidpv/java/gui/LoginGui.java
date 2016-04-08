package com.androidpv.java.gui;

import com.androidpv.java.database.dataBaseListener;

import javax.swing.*;

/**
 * Created by bradley on 4/8/2016.
 */
public class LoginGui extends JFrame{

    JButton blogin;
    JPanel loginpanel;
    JTextField txuser;
    JTextField pass;
    JButton newUSer;
    JLabel username;
    JLabel password;

    public static void main(String[] args){
        new LoginGui();
    }
    public LoginGui(){
        super("Login Authentication");
        dataBaseListener db = new dataBaseListener();
        blogin = new JButton("Login");
        loginpanel = new JPanel();
        txuser = new JTextField(15);
        pass = new JPasswordField(15);
        username = new JLabel("User - ");
        password = new JLabel("Pass - ");

        setSize(300,200);
        setLocation(500,280);
        loginpanel.setLayout (null);

        txuser.setBounds(70,30,150,20);
        pass.setBounds(70,65,150,20);
        blogin.setBounds(110,100,80,20);
        username.setBounds(20,28,80,20);
        password.setBounds(20,63,80,20);

        loginpanel.add(blogin);
        loginpanel.add(txuser);
        loginpanel.add(pass);
        loginpanel.add(username);
        loginpanel.add(password);

        getContentPane().add(loginpanel);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);


        blogin.addActionListener(e -> {
                String puname = txuser.getText();
                String ppasswd = pass.getText();

                if(db.checkMembership(puname , ppasswd)) {
                   new PVView();
                    dispose();
                }
                else if(puname.equals("") && ppasswd.equals("")){
                    JOptionPane.showMessageDialog(null,"Please insert Username and Password");
                }
                else {
                    JOptionPane.showMessageDialog(null,"Wrong Username / Password");
                    txuser.setText("");
                    pass.setText("");
                    txuser.requestFocus();
                }

        });

    }
 }

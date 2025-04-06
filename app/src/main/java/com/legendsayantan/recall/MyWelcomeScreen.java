package com.legendsayantan.recall;

import com.stephentuso.welcome.BasicPage;
import com.stephentuso.welcome.TitlePage;
import com.stephentuso.welcome.WelcomeActivity;
import com.stephentuso.welcome.WelcomeConfiguration;

public class MyWelcomeScreen extends WelcomeActivity {
    @Override
    protected WelcomeConfiguration configuration() {
        return new WelcomeConfiguration.Builder(this)
                .defaultBackgroundColor(R.color.purple_700)
                .page(new TitlePage(R.drawable.recalllogosmall,
                        "Welcome to ReCall !")
                )
                .page(new BasicPage(R.drawable.ic_baseline_phone_callback_24,
                        "Can't call me? Just Recallme !",
                        "Recieve calls from devices without a valid recharge !")
                        .background(R.color.purple_500)
                )
                .page(new BasicPage(R.drawable.ic_baseline_message_24,
                        "Never miss any emergency !",
                        "Get call request by just a message so you know when they need you...")
                )
                .animateButtons(true)
                .swipeToDismiss(true)
                .build();
    }
}
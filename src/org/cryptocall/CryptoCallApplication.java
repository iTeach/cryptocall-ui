/*
 * Copyright (C) 2012 Sergej Dechand <cryptocall@serj.de>
 *                    Dominik Schürmann <dominik@dominikschuermann.de>
 * 
 * This file is part of CryptoCall.
 * 
 * CryptoCall is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * CryptoCall is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with CryptoCall.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.cryptocall;

import org.cryptocall.service.ApgKeyServiceConnection;
import org.thialfihar.android.apg.service.IApgKeyService;

import android.app.Application;

public class CryptoCallApplication extends Application {
    public final ApgKeyServiceConnection mApgKeyServiceConnection = new ApgKeyServiceConnection(
            this);

    public IApgKeyService getApgKeyService() {
        return mApgKeyServiceConnection.getService();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Apg service
        mApgKeyServiceConnection.bindToApgKeyService();
    }

    @Override
    public void onTerminate() {
        mApgKeyServiceConnection.unbindFromApgKeyService();
    }

}

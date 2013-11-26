/*
 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

Based on Paul Blundell's Tutorial:
http://blog.blundell-apps.com/tut-asynctask-loader-using-support-library/

which is originally based on:
https://developer.android.com/reference/android/content/AsyncTaskLoader.html
 */

package net.binaryparadox.kerplapp;

import java.text.Collator;

public class Comparator {

    /**
     * Perform alphabetical comparison on entry objects
     */
    public static final java.util.Comparator<AppEntry> ALPHA_COMPARATOR = new java.util.Comparator<AppEntry>() {
        private final Collator collator = Collator.getInstance();

        @Override
        public int compare(AppEntry lhs, AppEntry rhs) {
            return collator.compare(lhs.getLabel(), rhs.getLabel());
        }
    };

}

/*
The MIT License (MIT)

Copyright (c) 2015, Hans-Georg Becker

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */

package de.tu_dortmund.ub.api.paaa.ils;

import de.tu_dortmund.ub.api.paaa.model.Block;
import de.tu_dortmund.ub.api.paaa.model.Fee;
import de.tu_dortmund.ub.api.paaa.model.Patron;

import java.util.HashMap;
import java.util.Properties;

/**
 * @author Hans-Georg Becker
 * @version 0.9 (2015-06-05)
 */
public interface IntegratedLibrarySystem {

    /**
     *
     * @param properties
     */
    void init(Properties properties);

    HashMap<String,String> health(Properties properties);

    Patron signup(Patron patron) throws ILSException;

    Patron newpatron(Patron patron) throws ILSException;

    Patron updatepatron(Patron patron) throws ILSException;

    Patron blockpatron(Patron patron, Block block) throws ILSException;

    Patron unblockpatron(Patron patron, Block block) throws ILSException;

    Patron deletepatron(Patron patron) throws ILSException;

    Fee newfee(Patron patron, Fee fee) throws ILSException;
}

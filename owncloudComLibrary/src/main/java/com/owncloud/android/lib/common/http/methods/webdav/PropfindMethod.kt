/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2020 ownCloud GmbH.
 *
 *   Permission is hereby granted, free of charge, to any person obtaining a copy
 *   of this software and associated documentation files (the "Software"), to deal
 *   in the Software without restriction, including without limitation the rights
 *   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *   copies of the Software, and to permit persons to whom the Software is
 *   furnished to do so, subject to the following conditions:
 *
 *   The above copyright notice and this permission notice shall be included in
 *   all copies or substantial portions of the Software.
 *
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 *   EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *   MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *   NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 *   BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 *   ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 *   CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *   THE SOFTWARE.
 *
 */
package com.owncloud.android.lib.common.http.methods.webdav

import at.bitfire.dav4jvm.Property
import at.bitfire.dav4jvm.Response
import at.bitfire.dav4jvm.Response.HrefRelation
import at.bitfire.dav4jvm.exception.DavException
import java.io.IOException
import java.net.URL
import java.util.ArrayList

/**
 * Propfind calls wrapper
 *
 * @author David González Verdugo
 */
class PropfindMethod(
    url: URL,
    // request
    val depth: Int,
    private val mPropertiesToRequest: Array<Property.Name>
) : DavMethod(url) {

    // response
    private val mMembers: MutableList<Response>
    var root: Response?
        private set

    @Throws(IOException::class, DavException::class)
    public override fun onExecute(): Int {
        mDavResource.propfind(
            depth = depth,
            reqProp = *mPropertiesToRequest,
            listOfHeaders = super.getRequestHeadersAsHashMap(),
            callback = { response: Response, hrefRelation: HrefRelation? ->
                when (hrefRelation) {
                    HrefRelation.MEMBER -> mMembers.add(response)
                    HrefRelation.SELF -> this.root = response
                    HrefRelation.OTHER -> {
                    }
                    else -> {
                    }
                }
            }, rawCallback = { callBackResponse: okhttp3.Response ->
                response = callBackResponse
            })
        return statusCode
    }

    val members: List<Response>
        get() = mMembers

    init {
        mMembers = ArrayList()
        this.root = null
    }
}
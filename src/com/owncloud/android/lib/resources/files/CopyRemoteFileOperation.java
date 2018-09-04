/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2016 ownCloud GmbH.
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

package com.owncloud.android.lib.resources.files;

import android.util.Log;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.http.HttpConstants;
import com.owncloud.android.lib.common.http.methods.webdav.CopyMethod;
import com.owncloud.android.lib.common.network.WebdavUtils;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.lib.resources.status.OwnCloudVersion;

import org.apache.commons.httpclient.HttpStatus;

import okhttp3.HttpUrl;


/**
 * Remote operation moving a remote file or folder in the ownCloud server to a different folder
 * in the same account.
 * <p/>
 * Allows renaming the moving file/folder at the same time.
 *
 * @author David A. Velasco
 */
public class CopyRemoteFileOperation extends RemoteOperation {

    private static final String TAG = CopyRemoteFileOperation.class.getSimpleName();

    private static final int COPY_READ_TIMEOUT = 600000;
    private static final int COPY_CONNECTION_TIMEOUT = 5000;

    private String mSrcRemotePath;
    private String mTargetRemotePath;

    private boolean mOverwrite;


    /**
     * Constructor.
     * <p/>
     * TODO Paths should finish in "/" in the case of folders. ?
     *
     * @param srcRemotePath    Remote path of the file/folder to move.
     * @param targetRemotePath Remove path desired for the file/folder after moving it.
     */
    public CopyRemoteFileOperation(String srcRemotePath, String targetRemotePath, boolean overwrite
    ) {
        mSrcRemotePath = srcRemotePath;
        mTargetRemotePath = targetRemotePath;
        mOverwrite = overwrite;
    }


    /**
     * Performs the rename operation.
     *
     * @param client Client object to communicate with the remote ownCloud server.
     */
    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {

        OwnCloudVersion version = client.getOwnCloudVersion();
        boolean versionWithForbiddenChars =
                (version != null && version.isVersionWithForbiddenCharacters());

        /// check parameters
        if (!FileUtils.isValidPath(mTargetRemotePath, versionWithForbiddenChars)) {
            return new RemoteOperationResult(ResultCode.INVALID_CHARACTER_IN_NAME);
        }

        if (mTargetRemotePath.equals(mSrcRemotePath)) {
            // nothing to do!
            return new RemoteOperationResult(ResultCode.OK);
        }

        if (mTargetRemotePath.startsWith(mSrcRemotePath)) {
            return new RemoteOperationResult(ResultCode.INVALID_COPY_INTO_DESCENDANT);
        }

        /// perform remote operation
        RemoteOperationResult result = null;
        try {
            CopyMethod copyMethod = new CopyMethod(
                    HttpUrl.parse(client.getWebdavUri() + WebdavUtils.encodePath(mSrcRemotePath)),
                    client.getWebdavUri() + WebdavUtils.encodePath(mTargetRemotePath),
                    mOverwrite);
            //TODO: apply timeout
            final int status = client.executeHttpMethod(copyMethod);

            if(status == HttpConstants.HTTP_CREATED || status == HttpConstants.HTTP_NO_CONTENT) {
                result = new RemoteOperationResult(ResultCode.OK);
            } else if (status == HttpStatus.SC_PRECONDITION_FAILED && !mOverwrite) {

                result = new RemoteOperationResult(ResultCode.INVALID_OVERWRITE);
                client.exhaustResponse(copyMethod.getResponseAsStream());


                /// for other errors that could be explicitly handled, check first:
                /// http://www.webdav.org/specs/rfc4918.html#rfc.section.9.9.4

            } else {
                result = new RemoteOperationResult(copyMethod);
                client.exhaustResponse(copyMethod.getResponseAsStream());
            }

            Log.i(TAG, "Copy " + mSrcRemotePath + " to " + mTargetRemotePath + ": " +
                    result.getLogMessage());

        } catch (Exception e) {
            result = new RemoteOperationResult(e);
            Log.e(TAG, "Copy " + mSrcRemotePath + " to " + mTargetRemotePath + ": " +
                    result.getLogMessage(), e);

        }

        return result;
    }
}

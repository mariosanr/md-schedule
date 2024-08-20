package com.stillloading.mdschedule.data

import android.net.Uri
import android.os.Build
import android.os.Parcel
import android.os.Parcelable

data class DirectoryData(
    val uri: Uri?,
    val text: String,
    var position: Int = 0
) : Parcelable {
    constructor(parcel: Parcel) : this(
        if(Build.VERSION.SDK_INT >= 33) parcel.readParcelable(Uri::class.java.classLoader, Uri::class.java) else parcel.readParcelable(Uri::class.java.classLoader),
        parcel.readString().toString(),
        parcel.readInt()
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(uri, flags)
        parcel.writeString(text)
        parcel.writeInt(position)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<DirectoryData> {
        override fun createFromParcel(parcel: Parcel): DirectoryData {
            return DirectoryData(parcel)
        }

        override fun newArray(size: Int): Array<DirectoryData?> {
            return arrayOfNulls(size)
        }
    }
}
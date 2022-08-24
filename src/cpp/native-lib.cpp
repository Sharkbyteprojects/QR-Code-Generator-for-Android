#include <jni.h>
#include <string>
#include <cstdlib>
#include <memory>
#include "external_libs/qrcodegen.hpp"

using std::uint8_t;
using qrcodegen::QrCode;

size_t pow(size_t in){
    return in * in;
}

size_t calcpoint(size_t x, size_t y, size_t max){
    return x + (y * max);
}

extern "C"
JNIEXPORT jintArray JNICALL
Java_com_sharkbyte_qrcodegenerator_MainActivity_qrFromStr(JNIEnv *env, jobject thiz, jstring in, jint size,
                                                          jint rgb_fg, jint rgb_bg,
                                                          jint failcorrection, jint border) {
    QrCode::Ecc errCorLvl =
            failcorrection ==  0 ?
            QrCode::Ecc::LOW :
            (failcorrection ==  1 ? QrCode::Ecc::MEDIUM : QrCode::Ecc::HIGH);  // Error correction level
    // Make and print the QR Code symbol
    const char* usableThing = env->GetStringUTFChars(in, 0);
    const QrCode qr = QrCode::encodeText(usableThing, errCorLvl);
    env->ReleaseStringUTFChars(in, usableThing);
    auto qrsize = qr.getSize();

    size_t width = border * 2 + qrsize,
     w2 = width * size,
     sizeOfD = pow(w2);
    int* returnD = (int*)malloc(sizeOfD * sizeof(int));
    if(returnD == nullptr){
        return env->NewIntArray(1);
    }

    {
        int y_set = 0,
            x_set = 0;
        for (int y = 0; y < width; y++) {
            for (int x = 0; x < width; x++) {
                //P START
                for (int ys = 0;ys < size;ys++) {
                    for(int xs = 0;xs < size;xs++) {
                        size_t pos = calcpoint(x_set + xs, y_set + ys, w2);
                        if (pos > sizeOfD) break;
                        returnD[pos] = qr.getModule(x - border, y - border) ? rgb_fg : rgb_bg;
                    }
                }
                x_set += size;
                //P END
            }
            y_set += size - 1;
        }
    }

    jintArray result = env->NewIntArray(sizeOfD);
    env->SetIntArrayRegion( result, 0, sizeOfD, (const jint*)returnD);
    free(returnD);
    return result;
}
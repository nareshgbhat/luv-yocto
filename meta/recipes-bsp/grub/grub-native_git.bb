SUMMARY = "GRUB2 is the next-generation GRand Unified Bootloader"

DESCRIPTION = "GRUB2 is the next generaion of a GPLed bootloader \
intended to unify bootloading across x86 operating systems. In \
addition to loading the Linux kernel, it implements the Multiboot \
standard, which allows for flexible loading of multiple boot images. \
This recipe builds an EFI binary for the target. It does not install \
or package anything, it only deploys a target-arch GRUB EFI image."

HOMEPAGE = "http://www.gnu.org/software/grub/"
SECTION = "bootloaders"

LICENSE = "GPLv3"
LIC_FILES_CHKSUM = "file://COPYING;md5=d32239bcb673463ab874e80d47fae504"

# FIXME: We should be able to optionally drop freetype as a dependency
DEPENDS = "autogen-native flex-native bison-native"
DEPENDS_class-target = "grub-native"
PR = "r0"

SRCREV = "72ec399ad8d6348b6c74ea63d80c79784c8b84ae"

SRC_URI = "git://git.savannah.gnu.org/grub;protocol=git \
           file://cfg \
          "

COMPATIBLE_HOST = '(x86_64.*|i.86.*)-(linux|freebsd.*)'

S = "${WORKDIR}/git"

inherit autotools-brokensep gettext texinfo deploy native

CACHED_CONFIGUREVARS += "ac_cv_path_HELP2MAN="
EXTRA_OECONF = "--with-platform=efi --disable-grub-mkfont \
                --enable-efiemu=no --program-prefix='' \
                --enable-liblzma=no --enable-device-mapper=no --enable-libzfs=no"

do_install_class-target() {
	:
}

do_install_class-native() {
	install -d ${D}${bindir}
	install -m 755 grub-mkimage ${D}${bindir}
}

do_configure () {
        export ac_cv_path_HELP2MAN=""
        ./autogen.sh
        ./configure ${EXTRA_OECONF}
}

do_deploy_class-native() {
	:
}

addtask do_install before do_build

FILES_${PN}-dbg += "${libdir}/${BPN}/${GRUB_TARGET}-efi/.debug"

ALLOW_EMPTY_${PN} = "1"

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
DEPENDS = "autogen-native"
RDEPENDS_${PN} = "diffutils freetype"
PR = "r4"

# Native packages do not normally rebuild when the target changes.
# Ensure this is built once per HOST-TARGET pair.
PN := "grub-efi-${TRANSLATED_TARGET_ARCH}-native"

SRCREV = "72ec399ad8d6348b6c74ea63d80c79784c8b84ae"

SRC_URI = "git://git.savannah.gnu.org/grub;protocol=git \
           file://cfg \
	   file://disable-help2man.patch \
          "

S = "${WORKDIR}/git"

# Determine the target arch for the grub modules before the native class
# clobbers TARGET_ARCH.

COMPATIBLE_HOST = '(x86_64.*|i.86.*)-(linux|freebsd.*)'

inherit autotools
inherit gettext
inherit native
inherit deploy

ORIG_TARGET_ARCH := "${TARGET_ARCH}"
python __anonymous () {
    import re
    target = d.getVar('ORIG_TARGET_ARCH', True)
    if target == "x86_64":
        grubtarget = 'x86_64'
        grubimage = "bootx64.efi"
    elif re.match('i.86', target):
        grubtarget = 'i386'
        grubimage = "bootia32.efi"
    else:
        raise bb.parse.SkipPackage("grub-efi is incompatible with target %s" % target)
    d.setVar("GRUB_TARGET", grubtarget)
    d.setVar("GRUB_IMAGE", grubimage)
}

EXTRA_OECONF = "--with-platform=efi --disable-grub-mkfont \
                --host=${HOST_PREFIX} --enable-efiemu=no --program-prefix='' \
                --enable-liblzma=no --enable-device-mapper=no --enable-libzfs=no \
                "

do_configure () {
        export ac_cv_path_HELP2MAN=""
	./autogen.sh
	./configure ${EXTRA_OECONF}
}

do_install () {
	oe_runmake exec_prefix="${STAGING_DIR_NATIVE}/usr" DESTDIR="${D}" install
}
addtask mkimage after do_compile before do_install

do_deploy() {
        install -m 644 ${B}/${GRUB_IMAGE} ${DEPLOYDIR}
}
addtask deploy after do_install before do_build

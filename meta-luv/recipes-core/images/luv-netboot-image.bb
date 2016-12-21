LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COREBASE}/LICENSE;md5=4d92cd373abda3937c2bc47fbc49d690"

DEPENDS_${PN} = "bits"

INITRD_IMAGE_LIVE = "core-image-efi-initramfs"
IMGDEPLOYDIR = "${DEPLOY_DIR_IMAGE}"

# bootimg.bbclass will set PCBIOS="1" if efi is not found in the MACHINE_FEATURES. For
# netboot we don't include 'efi' as a machine feature; this would imply the creation
# efi-related artifacts. We don't need them because this image is intended to be embedded
# into the grub image.
PCBIOS_remove = "1"
PCBIOS_append = "0"

APPEND = "debug crashkernel=256M console=ttyS0,115200 console=ttyPCH0,115200 luv.netboot ip=dhcp log_buf_len=1M"
LUVCFG_netconsole = "LUV_NETCONSOLE=10.11.12.13,64001"
LUVCFG_storage_url = "LUV_STORAGE_URL=http://ipaddress/cgi-bin/upload.php"
APPEND_aarch64 = "crashkernel=256M console=ttyAMA0 uefi_debug acpi=force luv.netboot"

HDDDIR = "${S}/hddimg"

inherit luv-efi
inherit image-live
inherit deploy

# Fool image-live into depend in xorriso instead of mkisofs to remove
# a dependency on syslinux, which does not build for aarch64.
python() {
    import re
    target = d.getVar('TARGET_ARCH', True)
    if re.match('aarch64', target):
        d.setVar('EFI_USEXORRISO', '1')
}

# reuse the same splash screen as in the disk live image
FILESEXTRAPATHS_append := "${THISDIR}/luv-live-image:"
SPLASH_IMAGE = "blue-luv.jpg"
SRC_URI = "file://blue-luv.jpg"

do_mkimage[depends] += "dosfstools-native:do_populate_sysroot \
                        mtools-native:do_populate_sysroot \
                        cdrtools-native:do_populate_sysroot \
                        virtual/kernel:do_deploy \
			${_BITSDEPENDS}"

do_bootimg[noexec] = "1"

do_populate_image() {
	install -d ${HDDDIR}${EFIDIR}
	if [ "${TARGET_ARCH}" != "aarch64" ]; then
		efi_populate_bits ${HDDDIR}
	else
		echo "bootaa64.efi" > ${HDDDIR}${EFIDIR}/startup.nsh
		install -m 0644 ${DEPLOY_DIR_IMAGE}/bootaa64.efi ${HDDDIR}${EFIDIR}
	fi
	install -m 0644 ${GRUBCFG} ${HDDDIR}${EFIDIR}
	install -m 0644 ${LUV_CFG} ${HDDDIR}
	build_hddimg
}

python do_mkimage() {
    bb.build.exec_func('build_efi_cfg', d)
    bb.build.exec_func('build_luv_cfg', d)
    bb.build.exec_func('do_populate_image', d)
    bb.build.exec_func('create_symlinks', d)
}

do_deploy() {
	rm -f ${DEPLOY_DIR_IMAGE}/${PN}.efi
	if [ "${TARGET_ARCH}" = "aarch64" ]; then
		ln -s ${DEPLOY_DIR_IMAGE}/bootaa64.efi ${DEPLOY_DIR_IMAGE}/${PN}.efi
	else
		ln -s ${DEPLOY_DIR_IMAGE}/bootx64.efi ${DEPLOY_DIR_IMAGE}/${PN}.efi
	fi
}

do_image_ext4() {
        :
}

do_image_ext4[noexec] = "1"

addtask do_mkimage before do_build
addtask do_deploy before do_build after do_mkimage
addtask image_ext4 before do_bootimg before do_build

do_mkimage[depends] += "${INITRD_IMAGE_LIVE}:do_build"
do_deploy[depends] += "${_RDEPENDS}:do_deploy"


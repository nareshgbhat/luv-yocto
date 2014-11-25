#
# Copyright (C) 2014 Intel Corporation.
#
# This is entirely specific to the Linux UEFI Validation (luv) project.
# We install a couple of boot loaders and a splash image.
#

do_bootimg[depends] += "grub-efi-${TRANSLATED_TARGET_ARCH}-native:do_deploy"

EFI_LOADER_IMAGE = "${@base_conditional('TARGET_ARCH', 'x86_64', 'bootx64.efi', 'bootia32.efi', d)}"
EFIDIR = "/EFI/BOOT"

GRUBCFG = "${S}/grub.cfg"

efi_populate() {
    # DEST must be the root of the image so that EFIDIR is not
    # nested under a top level directory.
    DEST=$1

    install -d ${DEST}${EFIDIR}
    install -d ${DEST}${EFIDIR}/bits

    # Install grub2 
    if [ "${TARGET_ARCH}" = "aarch64" ]; then
		install -m 0644 ${DEPLOY_DIR_IMAGE}/grubaa64.efi ${DEST}${EFIDIR}
    else
		install -m 0644 ${DEPLOY_DIR_IMAGE}/${EFI_LOADER_IMAGE} ${DEST}${EFIDIR}
    fi

    # Install BITS only for x86_64 architecture
    if [ "${TARGET_ARCH}" = "x86_64" ]; then
	    cp -r ${DEPLOY_DIR_IMAGE}/bits/boot ${DEST}
	    install -m 0644 ${DEPLOY_DIR_IMAGE}/bits/efi/boot/${EFI_LOADER_IMAGE} \
	        ${DEST}${EFIDIR}/bits/
    fi

    install -m 0644 ${GRUBCFG} ${DEST}${EFIDIR}

    install -m 0644 ${WORKDIR}/${SPLASH_IMAGE} ${DEST}${EFIDIR}
}

efi_iso_populate() {
    iso_dir=$1
    efi_populate $iso_dir
    # Build a EFI directory to create efi.img
    mkdir -p ${EFIIMGDIR}/${EFIDIR}
    cp -r $iso_dir/${EFIDIR}/* ${EFIIMGDIR}${EFIDIR}

    if [ "${TARGET_ARCH}" = "aarch64" ] ; then
        echo "grubaa64.efi" > ${EFIIMGDIR}/startup.nsh
        cp $iso_dir/Image ${EFIIMGDIR}
    fi
    if [ "${TARGET_ARCH}" = "x86_64" ] ; then
        echo "${GRUB_IMAGE}" > ${EFIIMGDIR}/startup.nsh
        cp $iso_dir/vmlinuz ${EFIIMGDIR}
    fi
    if [ -f "$iso_dir/initrd" ] ; then
        cp $iso_dir/initrd ${EFIIMGDIR}
    fi
}

efi_hddimg_populate() {
    efi_populate $1
}

python build_efi_cfg() {
    path = d.getVar('GRUBCFG', True)
    if not path:
        raise bb.build.FuncFailed('Unable to read GRUBCFG')

    try:
        cfgfile = file(path, 'w')
    except OSError:
        raise bb.build.funcFailed('Unable to open %s' % (cfgfile))

    # default is bits if architecture is x86_64
    cfgfile.write('default=bits\n')

    cfgfile.write('timeout=0\n')
    cfgfile.write('fallback=0\n')

    cfgfile.write('menuentry \'luv\' {\n')
   
    if "${TARGET_ARCH}" == "x86_64":
        cfgfile.write('linux /vmlinuz')

    if "${TARGET_ARCH}" == "aarch64":
        cfgfile.write('linux /Image')

    append = d.getVar('APPEND', True)
    if append:
        cfgfile.write('%s' % (append))

    cfgfile.write('\n')

    cfgfile.write('initrd /initrd\n')
    cfgfile.write('}\n')

    loader = d.getVar('EFI_LOADER_IMAGE', True)
    if not loader:
        raise bb.build.FuncFailed('Unable to find EFI_LOADER_IMAGE')

    cfgfile.write('menuentry \'bits\' {\n')
    
    if "${TARGET_ARCH}" == "x86_64":
        cfgfile.write('chainloader /EFI/BOOT/bits/%s\n' % loader)

    if "${TARGET_ARCH}" == "aarch64":
        cfgfile.write('chainloader /EFI/BOOT/bits/grubaa64.efi \n')

    cfgfile.write('}\n')

    cfgfile.close()
}

PR .= ".1"

GRUB_TARGET_aarch64 = "arm64"
GRUB_IMAGE_aarch64 = "grubaa64.efi"

do_mkimage() {
    # Search for the grub.cfg on the local boot media by using the
    # built in cfg file provided via this recipe
    grub-mkimage -c ../cfg -p /EFI/BOOT -d ./grub-core/ \
	-O ${GRUB_TARGET}-${GRUBPLATFORM} -o ./${GRUB_IMAGE} \
	boot linux ext2 fat serial part_msdos part_gpt \
	normal efi_gop iso9660 search efinet tftp all_video chain \
	gfxmenu jpeg gfxterm
}

addtask mkimage after do_compile before do_install

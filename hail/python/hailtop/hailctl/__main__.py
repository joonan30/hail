import os
import sys

import argparse
import re
import time

from hailtop import hailctl


def print_help():
    main_parser = argparse.ArgumentParser(prog='hailctl',
                                          description='Manage and monitor Hail deployments.')
    subs = main_parser.add_subparsers()

    subs.add_parser('dataproc',
                    help='Manage Google Dataproc clusters configured for Hail.',
                    description='Manage Google Dataproc clusters configured for Hail.')
    subs.add_parser('dev',
                    help='Manage Hail development utilities.',
                    description='Manage Hail development utilities.')
    subs.add_parser('version',
                    help='Print version information and exit.',
                    description='Print version information and exit.')
    subs.add_parser('batch',
                    help='Manage batches running on the batch service managed by the Hail team.',
                    description='Manage batches running on the batch service managed by the Hail team.')

    main_parser.print_help()


def check_for_update():
    try:
        check_file = os.path.expanduser('~') + '/.hail_version_check'
        if os.path.exists(check_file):
            last_modified = os.stat(check_file).st_ctime_ns

            delta = time.time() - last_modified / 10 ** 9
            assert delta > 0
            day = 60 * 60 * 24
            check_for_update = delta / day > 1
        else:
            check_for_update = True

        if check_for_update:
            open(check_file, 'w').close()  # touch the file

            import subprocess as sp
            try:
                pip_out = sp.check_output(['pip', 'search', 'hail'], stderr=sp.STDOUT)
            except Exception:  # pylint: disable=broad-except
                pip_out = sp.check_output(['pip3', 'search', 'hail'], stderr=sp.STDOUT)

            latest = re.search(r'hail \((\d+)\.(\d+)\.(\d+).*', pip_out.decode()).groups()
            installed = re.search(r'(\d+)\.(\d+)\.(\d+).*', hailctl.version()).groups()

            def int_version(version):
                return tuple(map(int, version))

            def fmt_version(version):
                return '.'.join(version)

            if int_version(latest) > int_version(installed):
                sys.stderr.write(f'~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n'
                                 f'You have Hail {fmt_version(installed)} installed, '
                                 f'but a newer version {fmt_version(latest)} exists.\n'
                                 f'  To upgrade to the latest version, please run:\n\n'
                                 f'    pip3 install -U hail\n\n'
                                 f'~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n\n')
    except Exception:  # pylint: disable=broad-except
        pass


def print_version():
    print(hailctl.version())


def main():
    check_for_update()

    if len(sys.argv) == 1:
        print_help()
        sys.exit(0)
    else:
        module = sys.argv[1]
        args = sys.argv[2:]
        if module == 'version':
            print_version()
        elif module == 'dataproc':
            from hailtop.hailctl.dataproc import cli
            cli.main(args)
        elif module == 'dev':
            from hailtop.hailctl.dev import cli
            cli.main(args)
        elif module == 'batch':
            from hailtop.hailctl.batch import cli
            cli.main(args)
        elif module in ('-h', '--help', 'help'):
            print_help()
        else:
            sys.stderr.write(f"ERROR: no such module: {module!r}")
            print_help()
            sys.exit(1)


if __name__ == '__main__':
    main()

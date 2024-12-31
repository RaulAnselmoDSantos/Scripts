# dec/03/2024 10:44:56 by RouterOS 6.49.10
# software id = A206-TS66
#
# model = RB750Gr3
# serial number = HFM09ZJ0FPQ
/interface bridge
add comment="Bridge para rede LAN" name=bridge-lan
add name=bridge1 protocol-mode=none
/interface wireless security-profiles
set [ find default=yes ] supplicant-identity=MikroTik
/ip pool
add name=dhcp-pool ranges=192.168.0.10-192.168.0.200
/ip dhcp-server
add address-pool=dhcp-pool disabled=no interface=ether5 name=dhcp-lan
/snmp community
set [ find default=yes ] disabled=yes
add addresses=::/0 authentication-password=F1 encryption-password=F1 name=F1 \
    security=private
/interface bridge port
add bridge=bridge1 interface=ether4
add bridge=bridge1 interface=ether3
/ip address
add address=192.168.1.2/24 comment="WAN - Modem 1" interface=ether1 network=\
    192.168.1.0
add address=192.168.2.2/24 comment="WAN - Modem 2" interface=ether2 network=\
    192.168.2.0
add address=192.168.0.1/24 comment="Gerenciamento Principal" interface=ether5 \
    network=192.168.0.0
/ip dhcp-server network
add address=192.168.0.0/24 gateway=192.168.0.1
/ip dns
set allow-remote-requests=yes servers=8.8.8.8,8.8.4.4
/ip firewall filter
add action=drop chain=input comment="Permitir Web Proxy" disabled=yes \
    dst-port=8675 log=yes protocol=tcp
add action=accept chain=input comment="Permitir trfego LAN" disabled=yes log=\
    yes src-address=192.168.0.0/24
add action=accept chain=input comment="Permitir SSH para o servidor" \
    disabled=yes dst-port=2022 log=yes protocol=tcp src-address=\
    192.168.0.0/24
add action=accept chain=forward comment="Permitir Zabbix" disabled=yes \
    dst-port=10051 log=yes protocol=tcp src-address=192.168.0.0/24
add action=accept chain=input comment="Permitir DNS para a rede interna" \
    disabled=yes dst-port=53 log=yes protocol=udp src-address=192.168.0.0/24
add action=accept chain=input comment="Permitir DNS para a rede interna" \
    disabled=yes dst-port=53 log=yes protocol=tcp src-address=192.168.0.0/24
add action=drop chain=input comment="Bloquear DNS externo" disabled=yes \
    dst-port=53 log=yes protocol=udp
add action=drop chain=input comment="Bloquear DNS externo" disabled=yes \
    dst-port=53 log=yes protocol=tcp
add action=drop chain=input comment="Bloquear acesso externo via Modem 1" \
    disabled=yes in-interface=ether1
add action=drop chain=input comment="Bloquear acesso externo via Modem 2" \
    disabled=yes in-interface=ether2
add action=accept chain=input comment="Permitir acesso interno via LAN" \
    disabled=yes src-address=192.168.0.0/24
add action=accept chain=input comment="Permitir Winbox na rede interna" \
    disabled=yes dst-port=8291 protocol=tcp src-address=192.168.0.0/24
add action=accept chain=input disabled=yes in-interface=ether2
add action=accept chain=input disabled=yes dst-port=67-68 in-interface=ether5 \
    protocol=udp
add action=fasttrack-connection chain=forward connection-state=\
    established,related
/ip firewall nat
add action=masquerade chain=srcnat comment="Masquerade para Modem 1" \
    out-interface=ether1
add action=masquerade chain=srcnat comment="Masquerade para Modem 2" \
    out-interface=ether2
/ip proxy
set port=8675 src-address=192.168.0.239
/ip route
add comment="Gateway Modem 1" distance=1 gateway=192.168.1.1
add comment="Gateway Modem 2" disabled=yes distance=2 gateway=192.168.2.1
/snmp
set contact="Mikrotik da Assistencia" enabled=yes location=Caraguatatuba \
    trap-community=F1 trap-version=2
/system clock
set time-zone-name=America/Sao_Paulo
/system identity
set name=Mikrotik_Rede

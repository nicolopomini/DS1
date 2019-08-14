import argparse
import json
import random

argument_parser = argparse.ArgumentParser(description="Generator of commands for the Distributed System 1 project")
argument_parser.add_argument("-n", dest="n", type=int, help="Total number of nodes", action="store")
argument_parser.add_argument("-out", dest="output", type=str, help="Output file name (json format)", action="store", default="output.json")
argument_parser.add_argument("-c", dest="commands", type=int, help="Total number of commands to be generated (1 inject, 1 fail, c-2 requests)", action="store")
argument_parser.add_argument("-s", dest="seed", type=int, help="Seed of the random generator - Optional", action="store", default=None)

args = argument_parser.parse_args()
random.seed(args.seed)
output_file = args.output
if not output_file.endswith(".json"):
	output_file += ".json"
commands = []
# random node to be injected with the token
injected = random.randint(0, args.n - 1)
commands.append({"command": "i", "arg": str(injected)})
# select node to fail 
failed = random.randint(0, args.n - 1)
commands.append({"command": "f", "arg": str(failed)})
nodes = [i for i in range(args.n)]
for i in range(args.commands - 2):
	if len(nodes) == 0:
		break
	node = random.choice(nodes)
	commands.append({"command": "r", "arg": str(node)})
	nodes.remove(node)

json_output = {"commands": commands}
with open(output_file, 'w') as fp:
    json.dump(json_output, fp, indent=4)
